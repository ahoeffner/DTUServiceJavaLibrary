package dtu.services.library.config;

import java.util.Map;
import java.util.HashMap;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.RestClient;
import tools.jackson.dataformat.yaml.YAMLFactory;


public class Database
{
    public final String name;
    public final Map<String,Map<String,Object>> config;
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final static Map<String,Database> configurations = new ConcurrentHashMap<>();


    public static synchronized Database get(RestClient client, String name)
    {
        Database config = configurations.computeIfAbsent(name,p->new Database(client,name));
        if (config.failed()) configurations.remove(name);
        return(config);
    }


    @SuppressWarnings("unchecked")
    public Database(RestClient client, String name)
    {
        this.name = name;

        String url = Environment.DATASOURCES_URL + "/" + name + ".yaml";

        if (name != null)
        {
           String yaml = client
                .get()
                .uri(url)
                .retrieve()
                .body(String.class);

            Map<String,Object> response = mapper.readValue(yaml,Map.class);
            this.config = (Map<String,Map<String,Object>>) response.get("properties");
        }
        else
        {
            this.config = new HashMap<>();
        }
    }


    public boolean failed()
    {
        return(this.config.size() == 0);
    }
}
