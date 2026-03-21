package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import java.time.Duration;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import dtu.services.library.resources.OAuthProviders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;


public class OAuthProvider
{
    public final String type;
    public final String scope;
    public final String issuer;
    public final String provider;
    public final String tokenpath;
    public final String publicpath;
    public final JwtDecoder decoder;
    public final Map<String,Object> config;

    private final RestClient restclient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Logger log = LoggerFactory.getLogger(OAuthProviders.class);
    private final static Map<String,OAuthProvider> configurations = new ConcurrentHashMap<>();


    public static synchronized OAuthProvider get(String provider)
    {
        OAuthProvider config = configurations.computeIfAbsent(provider,p->new OAuthProvider(p));
        if (config.failed()) configurations.remove(provider);
        return(config);
    }


    @SuppressWarnings("unchecked")
    OAuthProvider(String provider)
    {
        String type = null;
        String scope = null;
        String issuer = null;
        String tokenpath = null;
        String publicpath = null;
        JwtDecoder decoder = null;
        Map<String,Object> config = null;

        try
        {
            String uri = Environment.OAUTH_URL + "/" + provider + ".yaml";

            String response = restclient
                .get()
                .uri(uri)
                .retrieve()
                .body(String.class);

            config = mapper.readValue(response,Map.class);

            config = (Map<String,Object>) config.get("oauth2");
            if (config == null) throw new IllegalArgumentException("Missing 'oauth2' key");

            config = (Map<String,Object>) config.get(Environment.TYPE);
            if (config == null) throw new IllegalArgumentException("Missing 'oauth2/"+Environment.TYPE+"' key");

            type = (String) config.get("type");
            scope = (String) config.get("scope");
            issuer = (String) config.get("issuer-uri");
            tokenpath = (String) config.get("token-endpoint");
            publicpath = (String) config.get("public-endpoint");

            // Don't wait forever, 5 seconds should be enough
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(5000));
            factory.setReadTimeout(Duration.ofSeconds(5000));
            RestTemplate faster = new RestTemplate(factory);

            switch (type)
            {
                case "oracle":
                    decoder = NimbusJwtDecoder.withJwkSetUri(issuer)
                    .restOperations(faster).build();
                    break;

                default:
                    decoder = NimbusJwtDecoder.withIssuerLocation(issuer)
                    .restOperations(faster).build();
                    break;
            }
        }
        catch (Exception e)
        {
            type = null;
            scope = null;
            issuer = null;
            config = null;
            decoder = null;
            tokenpath = null;
            publicpath = null;

            log.error("Unable to initialize OAuth service for provider {}: {}",provider, e.getMessage());
        }

        this.type = type;
        this.scope = scope;
        this.issuer = issuer;
        this.config = config;
        this.decoder = decoder;
        this.provider = provider;
        this.tokenpath = tokenpath;
        this.publicpath = publicpath;
    }


    private boolean failed()
    {
        return(type == null);
    }
}