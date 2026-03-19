package dtu.services.library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerSecurity
{

@Bean
public OpenAPI customOpenAPI() {
    final String securitySchemeName = "KeycloakAuth";
    String tokenUrl = "http://localhost:9002/realms/dtu-service-users/protocol/openid-connect/token";

    return new OpenAPI()
        // Use .addSecurityItem with a requirement that has an empty list (not null)
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(new Components()
            .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.OAUTH2)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER) // Explicitly tell it where the token goes
                .description("Direct login with Username and Password")
                .flows(new OAuthFlows()
                    .password(new OAuthFlow()
                        .tokenUrl(tokenUrl)
                        .scopes(new io.swagger.v3.oas.models.security.Scopes()) // Must be initialized
                    )
                )
            )
        );
}

}
