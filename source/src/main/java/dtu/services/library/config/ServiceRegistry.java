package dtu.services.library.config;

import java.util.Map;
import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.dataformat.yaml.YAMLFactory;


@Component
public class ServiceRegistry
{
    private final RestClient client;

    private static Map<String, Map<String, Map<String, String>>> registry;
    private final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());


    private ServiceRegistry(RestClient client)
    {
        this.client = client;
    }


    @PostConstruct
    @SuppressWarnings("unchecked")
    private void init()
    {
        String url = Environment.REGISTRY_URL;

        if (url != null)
        {
            String yaml = client
                .get()
                .uri(url)
                .retrieve()
                .body(String.class);

            registry = mapper.readValue(yaml,Map.class);

        }
    }

    public String getUrl(String service)
    {
        if (registry == null) throw new IllegalStateException("ServiceRegistry not initialized");

        try { return(registry.get("services").get(service).get("url")); }
        catch (Exception e) { throw new IllegalArgumentException("Service not found in registry: " + service,e); }
    }
}