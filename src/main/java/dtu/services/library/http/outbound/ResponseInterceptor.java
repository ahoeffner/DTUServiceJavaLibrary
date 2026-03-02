package dtu.services.library.http.outbound;


import org.slf4j.Logger;
import java.io.IOException;

import javax.crypto.spec.OAEPParameterSpec;

import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import dtu.services.library.config.OAuth2;
import dtu.services.library.context.ServiceHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


class ResponseInterceptor implements ClientHttpRequestInterceptor
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);


    @NonNull
    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body, @NonNull ClientHttpRequestExecution execution) throws IOException
    {
        String header = null;
        ServiceHeaders headers = ServiceHeaders.getHeaders();

        if (headers == null ||ServiceHeaders.getUser() == null)
            return(execution.execute(request, body));


        if (headers != null)
        {
            try
            {
                header = objectMapper.writeValueAsString(headers);
                request.getHeaders().add(ServiceHeaders.HEADER, header);

            }
            catch (JacksonException e)
            {
                log.error("Cannot parse header",e);
            }
        }

        if (ServiceHeaders.getUser() != ServiceHeaders.ANON)
            request.getHeaders().add(ServiceHeaders.USER, ServiceHeaders.getUser());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth)
        {
            String token = jwtAuth.getToken().getTokenValue();
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        else
        {
            String token = OAuth2.getToken();
            if (token != null) request.getHeaders().setBearerAuth(token);
        }

        ClientHttpResponse response = execution.execute(request,body);
        return(response);
    }
}