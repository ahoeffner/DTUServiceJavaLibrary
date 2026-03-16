package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.util.MultiValueMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.RestClient;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


@Configuration
@EnableWebSecurity
public class OAuth2
{
    private static Secrets secrets;

    private static final ThreadLocal<String> token = new ThreadLocal<>();
    private static final Map<String,OAuth2Service> services = new ConcurrentHashMap<>();


    OAuth2(Secrets secrets)
    {
        OAuth2.secrets = secrets;
    }


    public static void setIncomingProvider(String provider)
    {
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(secrets,p));
        service.init(provider);
    }


    public static String setOutgoingProvider(String provider)
    {
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(secrets,p));

        service.init(provider);
        String actkn = service.getServiceToken(provider);

        token.set(actkn);
        return(actkn);
    }


    public static String getToken()
    {
        return(token.get());
    }


    static class OAuth2Service
    {
        private String scope;
        private String secret;
        private String issuer;
        private String clientid;
        private String tokenpath;

        private long expiryTime = 0;
        private String cached = null;
        private boolean initialized = false;

        private NimbusJwtDecoder decoder;
        private Map<String,Object> claims;
        private Map<String,Object> config;

        private final RestClient restclient = RestClient.create();
        private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        private final Logger log = LoggerFactory.getLogger(OAuth2Service.class);


        OAuth2Service(Secrets secrets, String provider)
        {
        }


        public String issuer()
        {
            return(this.issuer);
        }


        public synchronized void init(String provider)
        {
            if (initialized) return;
            loadConfig(provider);
            initialized = true;
       }

        @SuppressWarnings("unchecked")
        private void loadConfig(String provider)
        {
            String uri = Environment.OAUTH_URL + "/" + provider + ".yaml";

            try
            {
                String config = restclient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

                this.config = mapper.readValue(config,Map.class);
                this.config = (Map<String,Object>) this.config.get("oauth2");
                this.config = (Map<String,Object>) this.config.get(Environment.TYPE);

                this.issuer = (String) this.config.get("issuer-uri");
                this.decoder = NimbusJwtDecoder.withIssuerLocation(this.issuer).build();
                this.decoder.setJwtValidator(JwtValidators.createDefault());
            }
            catch (Exception e)
            {
                services.remove(provider);
                log.error("Unable to load oauth settings for {} {}",uri,e.getMessage());
            }
        }


        @SuppressWarnings("unchecked")
        synchronized String getServiceToken(String provider)
        {
            if (this.config == null)
                return(null);

            if (cached != null && System.currentTimeMillis() < expiryTime)
                return(cached);

            try
            {
                Map<String,String> auth = secrets.getSecrets("oauth2/"+provider+"/"+Environment.TYPE);

                if (auth == null)
                {
                    log.error("No secrets found for {}",provider);
                    services.remove(provider);
                    return(null);
                }

                scope = (String) this.config.get("scope");

                secret = (String) auth.get("client-secret");
                clientid = (String) auth.get("client-id");

                tokenpath = (String) this.config.get("token-endpoint");

                RestClient client = RestClient.create();
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

                formData.add("scope", scope);
                formData.add("grant_type", "client_credentials");

                Map<String, Object> response = client.post()
                        .uri(tokenpath)
                        .headers(headers -> headers.setBasicAuth(clientid,secret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData)
                        .retrieve()
                        .body(Map.class);

                String actkn = (String) response.get("access_token");
                Number expiresIn = (Number) response.get("expires_in");

                if (expiresIn != null)
                    this.expiryTime = System.currentTimeMillis() + (expiresIn.longValue() * 1000) - 60000;

                if (decoder != null)
                    this.claims = decoder.decode(actkn).getClaims();

                this.cached = actkn;

                System.out.println("!!!!! "+claims+" !!!!!");
                return(actkn);
            }
            catch (Exception e)
            {
                log.error("Failed to fetch service token for {}: {}", provider, e.getMessage());
                return(null);
            }
        }
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll());

        if ("prod".equals(Environment.TYPE))
        {
            http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        }
        else
        {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return(http.build());
    }
}