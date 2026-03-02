package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.core.io.ClassPathResource;
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
class OAuth2
{
    private String issuer;
    private String secret;
    private String clientid;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Logger log = LoggerFactory.getLogger(OAuth2.class);


    @Bean
    public OAuth2Service oauth2Service()
    {
        return(new OAuth2Service());
    }


    @Bean
    @ConditionalOnProperty(name = "environment.type", havingValue = "prod")
    public JwtDecoder jwtDecoder()
    {
        return(NimbusJwtDecoder.withIssuerLocation(this.issuer).build());
    }


    public class OAuth2Service
    {
        private String token;
        private long expiryTime = 0;


        public OAuth2Service()
        {
            loadConfig();
        }


        @SuppressWarnings("unchecked")
        private void loadConfig()
        {
            try (InputStream is = new ClassPathResource("oauth.yaml").getInputStream())
            {
                Map<String, Object> config = mapper.readValue(is, Map.class);
                config = (Map<String, Object>) config.get("oauth2");
                config = (Map<String, Object>) config.get(Environment.TYPE);

                // Directly populating the outer class fields
                issuer = (String) config.get("issuer-uri");
                secret = (String) config.get("client-secret");
                clientid = (String) config.get("client-id");
            }
            catch (Exception e)
            {
                log.error("Unable to load oauth settings", e);
            }
        }


        @SuppressWarnings("unchecked")
        public synchronized String getServiceToken()
        {
            if (token != null && System.currentTimeMillis() < expiryTime)
                return(token);

            try
            {
                String endpoint = issuer + "/protocol/openid-connect/token";
                RestClient client = RestClient.create();

                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", "client_credentials");
                formData.add("client_id", clientid);
                formData.add("client_secret", secret);

                Map<String, Object> response = client.post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData)
                        .retrieve()
                        .body(Map.class);

                this.token = (String) response.get("access_token");
                Number expiresIn = (Number) response.get("expires_in");

                if (expiresIn != null)
                    this.expiryTime = System.currentTimeMillis() + (expiresIn.longValue() * 1000) - 60000;

                return(this.token);
            }
            catch (Exception e)
            {
                log.error("Failed to fetch service token", e);
                return null;
            }
        }
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
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