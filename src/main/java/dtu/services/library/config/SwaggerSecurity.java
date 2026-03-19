package dtu.services.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import dtu.services.library.resources.OAuthProviders;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerSecurity
{
    @Bean
    public OpenAPI customOpenAPI()
    {
        final String securitySchemeName = "KeycloakAuth";
        String tokenUrl = OAuthProviders.getLocalTokenUrl();

        return
        (
            new OpenAPI()
            .components
            (
                new io.swagger.v3.oas.models.Components().addSecuritySchemes
                (
                    securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.OAUTH2)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Login with Keycloak credentials")
                    .flows
                    (
                        new OAuthFlows()
                            .password
                            (
                                new OAuthFlow()
                                    .tokenUrl(tokenUrl)
                                    .scopes(new io.swagger.v3.oas.models.security.Scopes())
                            )
                    )
                )
            )
        );
    }

    /**
     * This customizer runs after all controllers (including Actuator) are loaded.
     * It manually injects the security requirement into every single operation found.
     */
    @Bean
    public OpenApiCustomizer globalSecurityCustomizer()
    {
        return
        (
            openApi ->
            {
                final String securitySchemeName = "KeycloakAuth";
                SecurityRequirement requirement = new SecurityRequirement().addList(securitySchemeName);

                // Force the requirement onto the global list
                openApi.addSecurityItem(requirement);

                // Force the requirement onto every individual path/operation
                if (openApi.getPaths() != null)
                {
                    openApi.getPaths().values().forEach(pathItem ->
                        pathItem.readOperations().forEach(operation ->
                            operation.addSecurityItem(requirement)));
                }
            }
        );
    }
}