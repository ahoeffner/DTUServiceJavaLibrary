package dtu.services.library.events;

import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;


@Component
public class DTUEvents
{
    private final KafkaTemplate<String,Object> kafka;


    private DTUEvents(KafkaTemplate<String,Object> kafka)
    {
        this.kafka = kafka;
    }


    public void publish(String topic, Object payload)
    {
        Message<Object> message = new Message<Object>(topic,payload);
        kafka.send(topic,message);
    }
}
