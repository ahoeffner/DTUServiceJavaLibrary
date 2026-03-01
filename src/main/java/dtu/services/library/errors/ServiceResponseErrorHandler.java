package dtu.services.library.errors;

import java.net.URI;
import org.slf4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;


public class ServiceResponseErrorHandler extends DefaultResponseErrorHandler
{
    private static final Logger log = LoggerFactory.getLogger(ServiceResponseErrorHandler.class);


    @Override
    public void handleError(@NonNull URI url, @NonNull HttpMethod method, @NonNull ClientHttpResponse response) throws IOException
    {
        String body = "";

        try (InputStream is = response.getBody())
        {
            if (is != null)
                body = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            log.error("Failed to read error response body", e);
        }

        /* If body is empty, use the HTTP status text (e.g., "Not Found") */
        if (body == null || body.isBlank()) body = response.getStatusText();

        throw(new ServiceException(response.getStatusCode(), body));
    }
}