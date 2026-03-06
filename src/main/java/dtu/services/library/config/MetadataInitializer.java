package dtu.services.library.config;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;


class MetadataInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    private static final Logger log = LoggerFactory.getLogger(MetadataInitializer.class);

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext context)
    {
        Map<String,Object> metadata = ManifestScanner.getMetadata();

        if (!metadata.isEmpty())
        {
            ConfigurableEnvironment env = context.getEnvironment();

            // Injects properties into the Environment before beans are created
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("manifestMetadata", metadata));

            String version = (String) metadata.get("service.version");
            String service = (String) metadata.get("spring.application.name");

            Environment.TYPE = getProfile(env);

            Environment.VAULT_URL   = resolve(env, "dtu.vault-url", "dtu.environments." + Environment.TYPE + ".vault-url");
            Environment.VAULT_ROLE  = resolve(env, "dtu.vault-role", "dtu.environments." + Environment.TYPE + ".vault-role");
            Environment.VAULT_USER  = resolve(env, "dtu.vault-user", "dtu.environments." + Environment.TYPE + ".vault-user");
            Environment.VAULT_MOUNT = resolve(env, "dtu.vault-mount", "dtu.environments." + Environment.TYPE + ".vault-mount");

            Environment.OAUTH_URL = resolve(env, "dtu.oauth-url", "dtu.environments." + Environment.TYPE + ".oauth-url");
            Environment.QUEUE_URL = resolve(env, "dtu.queue-url", "dtu.environments." + Environment.TYPE + ".queue-url");
            Environment.DATASOURCES_URL = resolve(env, "dtu.datasources-url", "dtu.environments." + Environment.TYPE + ".datasources-url");

            Environment.K8S_TOKEN_PATH = resolve(env, "dtu.k8s-token-path", "dtu.defaults.k8s-token-path");
            if (Environment.K8S_TOKEN_PATH == null) Environment.K8S_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

            log.info("Starting: {}, version {}, deployment type {}", service, version, Environment.TYPE);
        }
    }


    private String getProfile(ConfigurableEnvironment env)
    {
        List<String> active = Arrays.stream(env.getActiveProfiles())
                                .map(String::toLowerCase)
                                .toList();

        if (active.size() == 0) return("dev");
        if (active.contains("dev")) return("dev");
        if (active.contains("test")) return("test");
        if (active.contains("prod")) return("prod");

        return("dev");
    }



    private String resolve(ConfigurableEnvironment env, String overrideKey, String defaultPath)
    {
        String value = env.getProperty(overrideKey);

        if (value == null)
            value = env.getProperty(defaultPath);

        return(value);
    }
}