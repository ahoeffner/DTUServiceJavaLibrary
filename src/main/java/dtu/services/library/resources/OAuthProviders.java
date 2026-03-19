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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class OAuthProviders
{
    private Secrets secrets;
    private final Map<String,OAuth2Service> services = new ConcurrentHashMap<>();
    private final ThreadLocal<OAuth2Service[]> authentications = new ThreadLocal<OAuth2Service[]>();
    private final Logger log = LoggerFactory.getLogger(OAuthProviders.class);


    OAuthProviders(Secrets secrets)
    {
        this.secrets = secrets;
    }


    public String getToken()
    {
        OAuth2Service[] services = authentications.get();
        if (services == null || services[1] == null) return(null);
        return(services[1].getServiceToken());
    }


    public String getIncoming()
    {
        OAuth2Service[] services = authentications.get();
        if (services == null || services[0] == null) return(null);
        return(services[0].provider);
    }


    public String getOutgoing()
    {
        OAuth2Service[] services = authentications.get();
        if (services == null || services[1] == null) return(null);
        return(services[1].provider);
    }


    public String setOutgoing(String provider)
    {
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(this,secrets,p));
        String actkn = service.getServiceToken();

        if (authentications.get() != null) authentications.get()[1] = service;
        else authentications.set(new OAuth2Service[]{null,service});

        return(actkn);
    }


    public String getExposedEndpoint()
    {
        if (authentications.get() == null)
            authentications.set(new OAuth2Service[]{local(),null});

        OAuth2Service service = authentications.get()[0];
        return((String) service.exposed());
    }


    public String getIncomingTokenUrl()
    {
        if (authentications.get() == null)
            authentications.set(new OAuth2Service[]{local(),null});

        OAuth2Service service = authentications.get()[0];
        return((String) service.config.get("token-endpoint"));
    }


    public String getOutgoingTokenUrl()
    {
        if (authentications.get() == null)
            authentications.set(new OAuth2Service[]{null,local()});

        OAuth2Service service = authentications.get()[1];
        return((String) service.config.get("token-endpoint"));
    }


    public String getUser()
    {
        String user = "anonymous";

        OAuth2Service[] auths = authentications.get();

        if (auths == null || auths[0] == null || auths[0].claims == null)
            return(user);

        if (auths[0].type().equals("keycloak"))
        {
            user = (String) auths[0].claims.get("preferred_username");
            if (user == null) user = (String) auths[0].claims.get("sub");
            if (user == null) user = (String) auths[0].claims.get("email");
        }

        return((user == null) ? "anonymous" : user);
    }


    @SuppressWarnings("unchecked")
    public String[] getRoles()
    {
        if (authentications.get() == null)
            return(null);

        OAuth2Service service = authentications.get()[0];
        if (service == null || service.claims == null) return(new String[0]);

        if (service.type().equals("keycloak"))
        {
            Map<String, Object> realmAccess = (Map<String, Object>) service.claims.get("realm_access");

            if (realmAccess != null && realmAccess.containsKey("roles"))
            {
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                return(roles.toArray(new String[0]));
            }
        }

        return(new String[0]);
    }


    private OAuth2Service local()
    {
        String provider = "local";
        OAuth2Service service = services.computeIfAbsent(provider,p->new OAuth2Service(this,secrets,p));
        return(service);
    }


    JwtDecoder dynamicJwtDecoder()
    {
        return((token) ->
        {
            String provider = null;

            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            provider = attrs.getRequest().getHeader("X-OAuth-Provider");
            if (provider == null || provider.isBlank()) provider = "local";

            OAuth2Service service = services.computeIfAbsent(provider, p -> new OAuth2Service(this,secrets,p));
            authentications.set(new OAuth2Service[]{service, null});

            JwtDecoder delegate = service.getDecoder();

            if (delegate == null)
            {
                log.error("Auth Provider unreachable");
                throw new IllegalStateException("Auth Provider unreachable");
            }

            Jwt jwt = delegate.decode(token);
            service.claims = jwt.getClaims();

            return(jwt);
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
        private String publicpath;

        private volatile long expiryTime = 0;
        private volatile String cached = null;

        private final String provider;
        private final Secrets secrets;
        private final OAuthProviders parent;

        private Map<String,Object> config;
        private volatile JwtDecoder decoder;
        private volatile Map<String,Object> claims;

        private final RestClient restclient = RestClient.create();
        private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        private final Logger log = LoggerFactory.getLogger(OAuth2Service.class);


        OAuth2Service(OAuthProviders parent, Secrets secrets, String provider)
        {
            this.parent = parent;
            this.secrets = secrets;
            this.provider = provider;

            try {this.loadConfig();}
            catch (Exception e)
            {
                log.error("Unable to initialize OAuth service for provider {}: {}", provider, e.getMessage());
                throw new IllegalStateException("Failed to initialize provider", e);
            }
        }


        public String type()
        {
            return(this.type);
        }


        public String issuer()
        {
            return(this.issuer);
        }


        public String exposed()
        {
            return(this.publicpath);
        }


        Map<String,Object> claims()
        {
            return(this.claims);
        }


        public JwtDecoder getDecoder()
        {
            return(this.decoder);
        }


        @SuppressWarnings("unchecked")
        private void loadConfig()
        {
            String uri = Environment.OAUTH_URL + "/" + provider + ".yaml";

            String config = restclient
                .get()
                .uri(uri)
                .retrieve()
                .body(String.class);

            this.config = mapper.readValue(config,Map.class);

            this.config = (Map<String,Object>) this.config.get("oauth2");
            if (this.config == null) throw new IllegalArgumentException("Missing 'oauth2' key");

            this.config = (Map<String,Object>) this.config.get(Environment.TYPE);
            if (this.config == null) throw new IllegalArgumentException("Missing 'oauth2/"+Environment.TYPE+"' key");

            this.type = (String) this.config.get("type");
            this.scope = (String) this.config.get("scope");
            this.issuer = (String) this.config.get("issuer-uri");
            this.tokenpath = (String) this.config.get("token-endpoint");
            this.publicpath = (String) this.config.get("public-endpoint");

            switch (this.type)
            {
                case "oracle":
                    this.decoder = NimbusJwtDecoder.withJwkSetUri(this.issuer).build();
                    break;

                default:
                    this.decoder = NimbusJwtDecoder.withIssuerLocation(this.issuer).build();
                    break;
            }
        }


        @SuppressWarnings("unchecked")
        synchronized String getServiceToken()
        {
            if (this.config == null)
                return(null);

            if (cached != null && System.currentTimeMillis() < expiryTime)
                return(cached);

            try
            {
                Map<String,String> auth = this.secrets.getSecrets("oauth2/"+provider+"/"+Environment.TYPE);

                if (auth == null)
                {
                    log.error("No secrets found for {}",provider);
                    parent.services.remove(provider);
                    return(null);
                }

                secret = (String) auth.get("client-secret");
                clientid = (String) auth.get("client-id");

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
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(dynamicJwtDecoder())))
            .addFilterAfter((request, response, chain) ->
            {
                // Run and clean up
                try {chain.doFilter(request, response);}
                finally {authentications.remove();}
            },BearerTokenAuthenticationFilter.class);

        return(http.build());
    }
}