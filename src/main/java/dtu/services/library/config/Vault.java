package dtu.services.library.config;

import java.io.File;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.KubernetesAuthentication;
import static org.springframework.vault.authentication.KubernetesAuthenticationOptions.*;


@Configuration
@Component("VaultBean")
class Vault
{
    @Bean
    public VaultTemplate vault()
    {
        ClientAuthentication auth;

        URI uri = URI.create(Environment.VAULT_URL);
        VaultEndpoint endpoint = VaultEndpoint.from(uri);

        File k8sTokenFile = new File(Environment.K8S_TOKEN_PATH);

        if (Environment.TYPE.equals("prod"))
        {
            if (!k8sTokenFile.exists())
            {
                throw new IllegalStateException("Critical Error: Deployment type is 'prod' but " +
                    "service account token was not found at: " + Environment.K8S_TOKEN_PATH);
            }

            String role = Environment.VAULT_ROLE;
            RestTemplate template = new RestTemplate();
            auth = new KubernetesAuthentication(builder().role(role).build(),template);
        }
        else
        {
            auth = new TokenAuthentication(Environment.VAULT_USER);
        }

        return(new VaultTemplate(endpoint, auth));
    }
}