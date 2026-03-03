package dtu.services.library.metrics;

import java.util.List;


public record Metrics
(
    String serviceName,
    String serviceVersion,
    String libraryVersion,
    List<Endpoint> endpoints
)
{
    public record Endpoint
    (
        String path,
        String method,
        List<Status> status
    )
    {}

    public record Status(
        int code,
        long calls,
        double avgResponseTime,
        double minResponseTime,
        double maxResponseTime
    )
    {}
}
