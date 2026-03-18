package dtu.services.library.events;

public interface DTUEvents
{
    /**
     * Publishes a payload to a specific Kafka topic.
     * @param topic The destination topic name.
     * @param payload The data object to be sent.
     */
    void publish(String topic, Object payload);
}