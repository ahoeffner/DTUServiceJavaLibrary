package dtu.services.library.config.events;

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
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;


@Component
class TopicRouter implements BeanPostProcessor
{
    private final KafkaListenerEndpointRegistry registry;
    private record HandlerReference(Object bean, Method method) {}
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, List<HandlerReference>> routes = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(TopicRouter.class);


    public TopicRouter(KafkaListenerEndpointRegistry registry)
    {
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


    public void onMessage(Message message)
    {
        List<HandlerReference> handlers = routes.get(message.getTopic());
        if (handlers == null) return;

        for (HandlerReference handler : handlers)
        {
            try
            {
                // Get the type the method wants (e.g., Book.class)
                Class<?> targetType = handler.method.getParameterTypes()[0];

                // Convert the raw string value back to that object
                Object payload = mapper.convertValue(message.getValue(), targetType);

                handler.method.invoke(handler.bean, payload);
            }
            catch (Exception e)
            {
                log.error("An unexpected error has occurred",e);
            }
        }
    }
}