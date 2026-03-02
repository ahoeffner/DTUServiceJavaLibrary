package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;


@Service
public class Secrets
{
    private final VaultTemplate vault;
    private static final Logger log = LoggerFactory.getLogger(Secrets.class);


    Secrets(VaultTemplate vault)
    {
        this.vault = vault;
    }


    /**
     * Retrieves any secrets by its key.
     */
    public Map<String,String> getSecrets(String path)
    {
        String mount = Environment.VAULT_MOUNT;
        VaultResponse response = vault.opsForKeyValue(mount,KeyValueBackend.KV_2).get(path);

        if (response != null && response.getData() != null)
        {
            Map<String,Object> data = response.getData();
            if (data != null) return(cast(data));
        }

        log.error("Secrets not found at mount: <"+mount+"> with path: <"+path+">");
        return(null);
    }


    /**
     * Retrieves any secret attribute by its key.
     * The key must be partion.name ex. api-keys.api-key.test
     */
    public String getSecret(String path, String attribute)
    {
        String mount = Environment.VAULT_MOUNT;
        VaultResponse response = vault.opsForKeyValue(mount,KeyValueBackend.KV_2).get(path);

        if (response != null && response.getData() != null)
        {
            Map<String, Object> data = response.getData();
            if (data != null) return ((String) data.get(attribute));
        }

        log.error("Secrets not found at mount: <"+mount+"> with path: <"+path+">");
        return(null);
    }


    private Map<String,String> cast(Map<String,Object> data)
    {
        return
        (
            data.entrySet().stream().collect
            (
                java.util.stream.Collectors.toMap
                (
                    Map.Entry::getKey,
                    e -> (String) e.getValue()
                )
            )
        );
    }
}
