package dtu.services.library.http.outbound;


import org.slf4j.Logger;
import java.io.IOException;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpRequest;
import dtu.services.library.context.ServiceHeaders;
import dtu.services.library.resources.OAuthProviders;
import org.springframework.security.core.Authentication;
import org.springframework.http.client.ClientHttpResponse;
import dtu.services.library.http.inbound.RequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


public class ResponseInterceptor implements ClientHttpRequestInterceptor
{
    private final OAuthProviders providers;
    private static final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);


    public ResponseInterceptor(OAuthProviders providers)
    {
        this.providers = providers;
    }


    @NonNull
    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body, @NonNull ClientHttpRequestExecution execution) throws IOException
    {
        ServiceHeaders headers = ServiceHeaders.getHeaders();

        if (RequestInterceptor.pass(request.getURI().getPath()))
            return(execution.execute(request, body));

        if (headers != null)
        {
            try {headers.setHeaders(request);}
            catch (Exception e) {log.error("Cannot parse header",e);}
        }

        String token = resolveToken();
        if (token != null) request.getHeaders().setBearerAuth(token);

        ClientHttpResponse response = execution.execute(request,body);
        return(response);
    }


    /**
     * Prioritizes the user token if the provider matches or if no outgoing provider is set.
     * Otherwise, uses the service-to-service token.
     */
    private String resolveToken()
    {
        String in = providers.getIncomingProvider();
        String out = providers.getOutgoingProvider();

        // Scenario A: Use the incoming User's token
        if (out == null || (in != null && in.equals(out)))
            return(providers.getIncomingToken());

        // Scenario B: Use the Service-to-Service token (Client Credentials)
        // (Happens if Scenario A didn't apply or didn't find a user token)
        return(providers.getOutgoingToken());
    }
}