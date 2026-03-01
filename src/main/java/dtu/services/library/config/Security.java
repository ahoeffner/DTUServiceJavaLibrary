package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
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
class Security
{
    private static String issuer;
    private final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static final Logger log = LoggerFactory.getLogger(Security.class);


    @PostConstruct
    @SuppressWarnings("unchecked")

    private void init()
    {
                try
        {
            InputStream is = new ClassPathResource("oauth.yaml").getInputStream();
            Map<String, Object> config = mapper.readValue(is, Map.class);

            config = (Map<String, Object>) config.get("oauth");
            config = (Map<String, Object>) config.get(Environment.TYPE);
            config = (Map<String, Object>) config.get("resourceserver");
            config = (Map<String, Object>) config.get("jwt");
            issuer = (String) config.get("issuer-uri");
        }
        catch (Exception e)
        {
            log.error("Unable to load oauth settings",e);
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
