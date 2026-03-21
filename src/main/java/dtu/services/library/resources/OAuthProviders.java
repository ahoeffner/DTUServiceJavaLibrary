package dtu.services.library.resources;

import java.util.Map;
import org.slf4j.Logger;
import java.util.Hashtable;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import dtu.services.library.config.Environment;
import dtu.services.library.config.OAuthConfig;
import org.springframework.web.client.RestClient;
import dtu.services.library.errors.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.context.request.RequestContextHolder;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class OAuthProviders
{
    private final Secrets secrets;

    private static final String USER = "X-OAuth-User";
    private static final String PROVIDER = "X-OAuth-Provider";

    private final ThreadLocal<State> authentications = new ThreadLocal<State>();
    private final Logger log = LoggerFactory.getLogger(OAuthProviders.class);


    OAuthProviders(Secrets secrets)
    {
        this.secrets = secrets;
    }


    public String getToken()
    {
        State state = authentications.get();

        if (state == null || state.out() == null)
            return(null);

        return(state.out().getServiceToken());
    }


    public String getIncoming()
    {
        State state = authentications.get();
        if (state == null || state.in() == null) return(null);
        return(state.in().provider);
    }


    public String getOutgoing()
    {
        State state  = authentications.get();

        if (state == null || state.out() == null)
            return(null);

        return(state.out().provider);
    }


    public String setOutgoing(String provider)
    {
        OAuth2Service service = null;
        State state  = authentications.get();

        if (state == null)
        {
            state = new State();
            authentications.set(state);
        }

        service = state.get(provider);

        if (service == null)
            service = new OAuth2Service(this,secrets,provider);

        String actkn = service.getServiceToken();
        state.out(service);

        return(actkn);
    }


    public String getExposedEndpoint()
    {
        State state  = authentications.get();

        if (state == null || state.in() == null)
        {
            state = new State(local(),null);
            authentications.set(state);
        }

        return(state.in().config.publicpath);
    }


    public String getIncomingTokenUrl()
    {
        State state  = authentications.get();

        if (state == null || state.in() == null)
        {
            state = new State(local(),null);
            authentications.set(state);
        }

        return(state.in().config.tokenpath);
    }


    public String getOutgoingTokenUrl()
    {
        State state  = authentications.get();

        if (state == null || state.out() == null)
            return(null);

        return(state.out().config.tokenpath);
    }


    public String getUser()
    {
        String user = "anonymous";
        State state = authentications.get();

        if (state == null || state.in() == null || state.in().claims == null)
            return(user);


        if (state.in().config.type.equals("keycloak"))
        {
            user = (String) state.in().claims.get("preferred_username");
            if (user == null) user = (String) state.in().claims.get("sub");
            if (user == null) user = (String) state.in().claims.get("email");
        }

        return((user == null) ? "anonymous" : user);
    }


    @SuppressWarnings("unchecked")
    public String[] getRoles()
    {
        State state = authentications.get();

        if (state == null || state.in() == null || state.in().claims == null)
            return(new String[0]);

        if (state.in().config.type.equals("keycloak"))
        {
            Map<String, Object> realmAccess = (Map<String, Object>) state.in().claims.get("realm_access");

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
        OAuth2Service service = new OAuth2Service(this,secrets,provider);
        return(service);
    }


    JwtDecoder dynamicJwtDecoder()
    {
        return((token) ->
        {
            String user = null;
            String provider = null;

            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            provider = attrs.getRequest().getHeader(PROVIDER);
            if (provider == null || provider.isBlank()) provider = "local";

            OAuth2Service service = new OAuth2Service(this,secrets,provider);
            authentications.set(new State(service, null));

            JwtDecoder delegate = service.getDecoder();

            if (delegate == null)
            {
                String msg = "Auth Provider '"+provider+"' unreachable";
                log.error(msg);
                throw new AuthenticationServiceException(msg);
            }

            if (!provider.equals("local"))
            {
                user = attrs.getRequest().getHeader(USER);

                if (user == null || user.isBlank())
                    throw new AuthenticationServiceException("missing '"+USER+"'' header");
            }

            Jwt jwt = delegate.decode(token);
            service.claims = jwt.getClaims();

            return(jwt);
        });
    }


    private AuthenticationEntryPoint authEntryPoint()
    {
        return (request, response, authException) ->
        {
            response.setStatus(SC_UNAUTHORIZED);
            response.setContentType(APPLICATION_JSON_VALUE);

            ErrorResponse error = new ErrorResponse
            (
                UNAUTHORIZED,
                authException.getMessage() // This will show "Auth Provider unreachable" etc.
            );

            response.getWriter().write(error.toString());
        };
    }


    static class State
    {
        private OAuth2Service in = null;
        private OAuth2Service out = null;
        private Hashtable<String,OAuth2Service> services = new Hashtable<String,OAuth2Service>();


        State()
        {
            this(null,null);
        }



        State(OAuth2Service in, OAuth2Service out)
        {
            this.in = in;
            this.out = out;
        }


        public OAuth2Service out()
        {
            return(this.out);
        }


        public OAuth2Service in()
        {
            return(this.in);
        }


        public OAuth2Service get(String provider)
        {
            return(this.services.get(provider));
        }


        public State in(OAuth2Service service)
        {
            this.in = service;
            return(this);
        }


        public State out(OAuth2Service service)
        {
            if (service == out)
                return(this);

            if (this.out != null)
                this.services.put(this.out.provider,this.out);

            this.out = service;
            return(this);
        }
    }


    static class OAuth2Service
    {
        private volatile long expiryTime = 0;
        private volatile String cached = null;

        private final String provider;
        private final Secrets secrets;
        private final OAuthConfig config;
        private volatile Map<String,Object> claims;

        private final Logger log = LoggerFactory.getLogger(OAuth2Service.class);


        OAuth2Service(OAuthProviders parent, Secrets secrets, String provider)
        {
            this.secrets = secrets;
            this.provider = provider;
            this.config = OAuthConfig.get(provider);
        }


        Map<String,Object> claims()
        {
            return(this.claims);
        }


        public JwtDecoder getDecoder()
        {
            return(this.config.decoder);
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
                    return(null);
                }

                String secret = (String) auth.get("client-secret");
                String clientid = (String) auth.get("client-id");

                RestClient client = RestClient.create();
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

                formData.add("scope",config.scope);
                formData.add("grant_type", "client_credentials");

                Map<String, Object> response = client.post()
                        .uri(config.tokenpath)
                        .headers(headers -> headers.setBasicAuth(clientid,secret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData)
                        .retrieve()
                        .body(Map.class);

                String actkn = (String) response.get("access_token");
                Number expiresIn = (Number) response.get("expires_in");

                if (expiresIn != null)
                    this.expiryTime = System.currentTimeMillis() + (expiresIn.longValue() * 1000) - 60000;

                if (config.decoder != null)
                    this.claims = config.decoder.decode(actkn).getClaims();

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
            .exceptionHandling(eh -> eh.authenticationEntryPoint(authEntryPoint()))

            .oauth2ResourceServer(oauth ->oauth.jwt(jwt -> jwt.decoder(dynamicJwtDecoder())))

            .addFilterAfter((request, response, chain) ->
            {
                // Run and clean up
                try {chain.doFilter(request, response);}
                finally {authentications.remove();}
            },

            BearerTokenAuthenticationFilter.class);

        return(http.build());
    }
}