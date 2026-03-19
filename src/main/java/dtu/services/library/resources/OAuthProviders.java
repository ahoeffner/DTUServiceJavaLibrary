package dtu.services.library.resources;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.util.MultiValueMap;
import java.util.concurrent.ConcurrentHashMap;
import dtu.services.library.config.Environment;
import org.springframework.web.client.RestClient;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


@Configuration
@EnableWebSecurity
public class OAuthProviders
{
    private static Secrets secrets;
    private static final Map<String,OAuth2Service> services = new ConcurrentHashMap<>();
    private static final ThreadLocal<OAuth2Service[]> authentications = new ThreadLocal<OAuth2Service[]>();
    private static final Logger log = LoggerFactory.getLogger(OAuthProviders.class);


    OAuthProviders(Secrets secrets)
    {
        OAuthProviders.secrets = secrets;
    }


    private static synchronized OAuth2Service local()
    {
        String provider = "local";
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(secrets,p));
        service.init(provider);
        return(service);
    }


    public static synchronized String getToken()
    {
        OAuth2Service[] services = authentications.get();
        if (services == null || services[1] == null) return(null);
        return(services[1].getServiceToken(services[1].issuer()));
    }


    public static synchronized void resetIncoming()
    {
        OAuth2Service service = local();
        if (authentications.get() != null) authentications.get()[0] = service;
        else authentications.set(new OAuth2Service[]{service,null});
    }


    public static synchronized void setIncoming(String provider)
    {
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(secrets,p));
        service.init(provider);

        if (authentications.get() != null) authentications.get()[0] = service;
        else authentications.set(new OAuth2Service[]{service,null});
    }


    public static String setOutgoing(String provider)
    {
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(secrets,p));

        service.init(provider);
        String actkn = service.getServiceToken(provider);

        if (authentications.get() != null) authentications.get()[1] = service;
        else authentications.set(new OAuth2Service[]{null,service});

        return(actkn);
    }


    public static synchronized String getLocalTokenUrl()
    {
        OAuth2Service service = local();
        return (String) service.config.get("token-endpoint");
    }


    JwtDecoder dynamicJwtDecoder()
    {
        return((token) ->
        {
            OAuth2Service[] auths = authentications.get();

            if (auths == null || auths[0] == null)
            {
                resetIncoming();
                auths = authentications.get();
            }

            JwtDecoder delegate = auths[0].getDecoder();

            if (delegate == null)
            {
                log.error("Auth Provider unreachable");
                throw new IllegalStateException("Auth Provider unreachable");
            }

            return(delegate.decode(token));
        });
    }


    static class OAuth2Service
    {
        private String type;
        private String scope;
        private String secret;
        private String issuer;
        private String clientid;
        private String tokenpath;

        private long expiryTime = 0;
        private String cached = null;
        private boolean initialized = false;

        private JwtDecoder decoder;
        private Map<String,Object> claims;
        private Map<String,Object> config;

        private final RestClient restclient = RestClient.create();
        private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        private final Logger log = LoggerFactory.getLogger(OAuth2Service.class);


        OAuth2Service(Secrets secrets, String provider)
        {
        }


        public String type()
        {
            return(this.type);
        }


        public String issuer()
        {
            return(this.issuer);
        }


        Map<String,Object> claims()
        {
            return(this.claims);
        }


        public JwtDecoder getDecoder()
        {
            return(this.decoder);
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

                this.type = (String) this.config.get("type");
                this.issuer = (String) this.config.get("issuer-uri");
                this.decoder = NimbusJwtDecoder.withIssuerLocation(this.issuer).build();
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

        http
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(dynamicJwtDecoder())));

        return(http.build());
    }
}