package dtu.services.library.metrics;

import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.DoubleAdder;
import org.springframework.beans.factory.annotation.Value;


@Service
public class MetricsAggregator
{
    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${service.version}")
    private String serviceVersion;

    @Value("${library.version}")
    private String libraryVersion;

    private final Map<String, Map<String, Map<Integer, Stats>>> data = new ConcurrentHashMap<>();


    public void add(String path, String method, int code, long duration)
    {
        data.computeIfAbsent(path, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(method, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(code, k -> new Stats())
            .record(duration);
    }

    /**
     * Transforms the live data into your Metrics record structure
     */
    public Metrics getSnapshot()
    {
        List<Metrics.Endpoint> endpoints = data.entrySet().stream()
            .map(pathEntry ->
            {
                String path = pathEntry.getKey();

                List<Metrics.Endpoint> methods = pathEntry.getValue().entrySet().stream()
                    .map(methodEntry ->
                    {
                        String method = methodEntry.getKey();

                        List<Metrics.Status> statuses = methodEntry.getValue().entrySet().stream()
                            .map(statusEntry ->
                            {
                                int code = statusEntry.getKey();
                                Stats s = statusEntry.getValue();
                                long count = s.count.sum();

                                return new Metrics.Status
                                (
                                    code,
                                    count,
                                    count == 0 ? 0 : s.totalTime.sum() / count,
                                    count == 0 ? 0 : s.min.get(),
                                    s.max.get()
                                );
                            }).toList();

                        return(new Metrics.Endpoint(path, method, statuses));
                    }).toList();

                return(methods);
            })
            .flatMap(List::stream)
            .toList();

        return(new Metrics(serviceName, serviceVersion, libraryVersion, endpoints));
    }


    private static class Stats
    {
        private final LongAdder count = new LongAdder();
        private final DoubleAdder totalTime = new DoubleAdder();
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong max = new AtomicLong(0);

        public void record(long duration)
        {
            count.increment();
            totalTime.add(duration);
            min.updateAndGet(current -> Math.min(current, duration));
            max.updateAndGet(current -> Math.max(current, duration));
        }
    }
}