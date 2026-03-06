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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


@Configuration
@EnableWebSecurity
public class OAuth2
{
    private static String issuer;
    private static Secrets secrets;

    private static final ThreadLocal<String> token = new ThreadLocal<>();
    private static final Map<String,OAuth2Service> services = new ConcurrentHashMap<>();


    OAuth2(Secrets secrets)
    {
        OAuth2.secrets = secrets;
    }


    public static void setIncomingProvider(String provider)
    {
        OAuth2Service service = services.get(provider);

        if (service == null)
        {
            service = new OAuth2Service(secrets,provider);
            services.put(provider,service);
        }

        OAuth2.issuer = service.issuer();
    }


    public static String setOutgoingProvider(String provider)
    {
        OAuth2Service service = services.get(provider);

        if (service == null)
        {
            service = new OAuth2Service(secrets,provider);
            services.put(provider,service);
        }

        String actkn = service.getServiceToken(provider);
        token.set(actkn);

        return(actkn);
    }


    public static String getToken()
    {
        return(token.get());
    }


    @Bean
    @ConditionalOnProperty(name = "environment.type", havingValue = "prod")
    JwtDecoder jwtDecoder()
    {
        return(NimbusJwtDecoder.withIssuerLocation(this.issuer).build());
    }


    static class OAuth2Service
    {
        private String scope;
        private String secret;
        private String issuer;
        private String clientid;
        private String tokenpath;
        private long expiryTime = 0;
        private Map<String,Object> config;

        private final RestClient restclient = RestClient.create();
        private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        private final Logger log = LoggerFactory.getLogger(OAuth2Service.class);


        OAuth2Service(Secrets secrets, String provider)
        {
            loadConfig(provider);
        }


        public String issuer()
        {
            return(this.issuer);
        }


        @SuppressWarnings("unchecked")
        private void loadConfig(String provider)
        {
            try
            {
                String uri = Environment.OAUTH_URL + "/" + provider + ".yaml";

                String config = restclient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

                this.config = mapper.readValue(config,Map.class);
                this.config = (Map<String,Object>) this.config.get("oauth2");
                this.config = (Map<String,Object>) this.config.get(Environment.TYPE);
            }
            catch (Exception e)
            {
                log.error("Unable to load oauth settings", e);
            }
        }


        @SuppressWarnings("unchecked")
        synchronized String getServiceToken(String provider)
        {
            if (token != null && System.currentTimeMillis() < expiryTime)
                return(token.get());

            try
            {
                Map<String, Object> entry = config;
                entry = (Map<String, Object>) entry.get("oauth2");
                entry = (Map<String, Object>) entry.get(Environment.TYPE);
                Map<String,String> auth = secrets.getSecrets("oauth2/"+provider+"/"+Environment.TYPE);

                scope = (String) entry.get("scope");
                issuer = (String) entry.get("issuer-uri");

                secret = (String) auth.get("client-secret");
                clientid = (String) auth.get("client-id");

                tokenpath = (String) entry.get("token-endpoint");

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

                return(actkn);
            }
            catch (Exception e)
            {
                log.error("Failed to fetch service token {}",e.getMessage());
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