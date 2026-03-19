package dtu.services.library.context;


import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceHeaders
{
    @JsonProperty("user")
    private String user;

    @JsonProperty("application")
    private String application;

    public static final String HEADER = "X-Service-Context";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ThreadLocal<ServiceHeaders> client = new ThreadLocal<>();


    public static void setHeaders(ServiceHeaders headers)
    {
        client.set(headers);
    }


    public static ServiceHeaders getHeaders()
    {
        return(client.get());
    }


    public ServiceHeaders(String application)
    {
        this.application = application;
    }


    public static void clear()
    {
        client.remove();
    }


    public String getUser()
    {
        return(this.user);
    }


    public void setUser(String user)
    {
        this.user = user;
    }


    public String getApplication()
    {
        return(this.application);
    }


    public void setApplication(String application)
    {
        this.application = application;
    }


    public static ServiceHeaders extract(String service, HttpServletRequest request) throws Exception
    {
        ServiceHeaders headers = null;

        String header = request.getHeader(HEADER);

        headers = (header != null && !header.isBlank())
            ? objectMapper.readValue(header, ServiceHeaders.class)
            : new ServiceHeaders(service);

        client.set(headers);
        return(headers);
    }


    public void setHeaders(HttpServletResponse response) throws Exception
    {
        String header = objectMapper.writeValueAsString(this);
        response.setHeader(HEADER,header);
    }



    @SuppressWarnings("unchecked")
    public void setHeaders(HttpRequest request) throws Exception
    {
        Map<String,Object> headers = objectMapper.convertValue(this,Map.class);
        if (headers.containsKey("user"))headers.remove("user");
        String header = objectMapper.writeValueAsString(this);
        request.getHeaders().set(HEADER,header);
    }
}