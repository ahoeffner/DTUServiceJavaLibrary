package dtu.services.library.context;


import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceHeaders
{
    @JsonProperty("user")
    private String username;

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


    public static void clear()
    {
        client.remove();
    }


    public String getUser()
    {
        return(client.get().username);
    }


    public void setUser(String username)
    {
        client.get().username = username;
    }


    public static String getApplication()
    {
        return(client.get().application);
    }


    public static void setApplication(String application)
    {
        client.get().application = application;
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


    public ServiceHeaders(String application)
    {
        this.application = application;
    }


    public void setHeaders(HttpServletResponse response) throws Exception
    {
        String header = objectMapper.writeValueAsString(this);
        response.setHeader(HEADER,header);
    }
}