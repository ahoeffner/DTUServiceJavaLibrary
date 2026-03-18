package dtu.services.library.events;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;


@Component
class KafkaDTUEvents implements DTUEvents
{
    private final KafkaTemplate<String,Object> kafka;

    public KafkaDTUEvents(KafkaTemplate<String, Object> kafka)
    {
        this.kafka = kafka;
    }

    @Override
    public void publish(String topic, Object payload)
    {
        Objects.requireNonNull(topic, "Cannot publish to a null topic");
        Objects.requireNonNull(payload, "Cannot publish a null payload");

        Message<Object> message = new Message<>(topic, payload);
        kafka.send(topic, message);
    }
}