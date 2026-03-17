package dtu.services.library.http.inbound;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dtu.services.library.context.ServiceContext;
import dtu.services.library.context.ServiceHeaders;
import org.springframework.web.servlet.HandlerInterceptor;


// This acts like a 'finally'. Just to be 100%.
// Note: This must be registered first in the interceptor chain to ensure it runs even if subsequent interceptors fail.
// This is guaranteed by the HIGHEST_PRECEDENCE when registered i the Configuration class.

class FinalInterceptor implements HandlerInterceptor
{
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex)
    {
        ServiceHeaders.clear();
        ServiceContext.clear();
    }
}