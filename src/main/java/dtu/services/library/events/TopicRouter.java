package dtu.services.library.events;

import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Lazy;
import dtu.services.library.metrics.MetricsAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import static net.logstash.logback.argument.StructuredArguments.kv;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;



@Component
class TopicRouter implements BeanPostProcessor
{
    @Value("${service.version}")
    private String version;

    @Value("${spring.application.name}")
    private String service;

    private final MetricsAggregator metrics;
    private final KafkaListenerEndpointRegistry registry;
    private record HandlerReference(Object bean, Method method) {}
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, List<HandlerReference>> routes = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(TopicRouter.class);


    public TopicRouter(@Lazy KafkaListenerEndpointRegistry registry, @Lazy MetricsAggregator metrics)
    {
        this.metrics = metrics;
        this.registry = registry;
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String name)
    {
        for (Method method : bean.getClass().getDeclaredMethods())
        {
            if (method.isAnnotationPresent(DTUSubscriber.class))
            {
                String topic = method.getAnnotation(DTUSubscriber.class).value();

                routes.computeIfAbsent(topic, k -> new ArrayList<>())
                      .add(new HandlerReference(bean, method));
            }
        }
        return(bean);
    }


    public void stop()
    {
        registry.stop();
    }


    public String[] getRegisteredTopics()
    {
        return(routes.keySet().toArray(new String[0]));
    }


    public void onMessage(Message<Object> message)
    {
        List<HandlerReference> handlers = routes.get(message.getTopic());
        if (handlers == null) return;

        long time = 0l;
        String clazz  = null;
        String method = null;

        for (HandlerReference handler : handlers)
        {
            try
            {
                clazz = null;
                method = null;
                time = System.currentTimeMillis();

                // Get the type the method wants (e.g., Employee.class)
                Class<?> type = handler.method.getParameterTypes()[0];

                // Convert the raw string value back to that object
                Object payload = mapper.convertValue(message.getPayload(),type);

                method = handler.method.getName();
                clazz  = handler.method.getDeclaringClass().getSimpleName();

                handler.method.invoke(handler.bean,payload);
                time = System.currentTimeMillis() - time;

                metrics.add(clazz,method ,200,time);

                log.info("Metrics",
                    kv("service",this.service),
                    kv("version",this.version),
                    kv("method", method),
                    kv("status", 200),
                    kv("path", clazz),
                    kv("time", time));
            }
            catch (Exception e)
            {
                if (clazz == null || method == null)
                {
                    time = System.currentTimeMillis() - time;
                    metrics.add(clazz,method ,500,time);

                    log.info("Metrics",
                        kv("service",this.service),
                        kv("version",this.version),
                        kv("method", method),
                        kv("status", 500),
                        kv("path", clazz),
                        kv("time", time));
                }

                log.error("An unexpected error has occurred",e);
            }
        }
    }
}