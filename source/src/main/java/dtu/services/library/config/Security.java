package dtu.services.library.config;

import tools.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.web.client.RestClient;
import org.springframework.context.annotation.Bean;
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
class Security
{
    private final RestClient client;

    private static String issuer;
    private static JsonNode oauth;
    private final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());



    Security(RestClient client)
    {
        this.client = client;
    }


    @PostConstruct
    private void init()
    {
        String url = Environment.OAUTH_URL;

        if (url != null)
        {
            String yaml = client
                .get()
                .uri(url)
                .retrieve()
                .body(String.class);

            oauth = mapper.readTree(yaml);

            issuer = oauth
                .path("oauth2")
                .path("resourceserver")
                .path("jwt")
                .path("issuer-uri").toString();
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        // Always allow Swagger/OpenAPI
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll());

        if (Environment.TYPE.equals("prod"))
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

    @Bean
    @ConditionalOnProperty(name = "environment.type", havingValue = "prod")
    public JwtDecoder jwtDecoder()
    {
        return(NimbusJwtDecoder.withIssuerLocation(issuer).build());
    }
}
