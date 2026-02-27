package dtu.services.library.http.inbound;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dtu.services.library.context.ServiceHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import static net.logstash.logback.argument.StructuredArguments.kv;


class RequestInterceptor implements HandlerInterceptor
{
    private final String service;
    private static final Logger log = LoggerFactory.getLogger(RequestInterceptor.class);

    private static final List<String> EXCLUDE = List.of
    (
        "/swagger-ui",
        "/v3/api-docs",
        "/favicon.ico"
    );



    public RequestInterceptor(String service)
    {
        this.service = service;
    }


    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
    {
        ServiceContext.create();

        String path = request.getRequestURI();
        if (EXCLUDE.stream().anyMatch(path::startsWith)) return(true);

        try
        {
            ServiceHeaders headers = ServiceHeaders.extract(service,request);
            headers.setHeaders(response);
        }
        catch (Exception e)
        {
            response.setStatus(400);
            log.error("Invalid "+ServiceHeaders.HEADER+" JSON provided: ", e);
            afterCompletion(request,response,handler,e);
            return(false);
        }

        return(true);
    }


    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex)
    {
        ServiceHeaders headers = ServiceHeaders.getHeaders();
        if (headers != null)
        {
            ServiceContext state = ServiceContext.getContext();
            Long time = state.finish();

            log.info("Metrics",
                kv("service",this.service),
                kv("method", request.getMethod()),
                kv("status", response.getStatus()),
                kv("path", request.getRequestURI()),
                kv("time", time));
        }

        ServiceContext.clear();
        ServiceHeaders.clear();
    }
}