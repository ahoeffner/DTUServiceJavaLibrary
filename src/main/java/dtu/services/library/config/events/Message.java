package dtu.services.library.config.events;


class Message<T>
{
    private String topic;
    private T payload;

    // Jackson needs a non-null default constructor
    public Message() {}

    public Message(String topic, T value)
    {
        this.topic = topic;
        this.payload = value;
    }

    public String getTopic() { return topic; }
    public T getPayload() { return payload; }
}