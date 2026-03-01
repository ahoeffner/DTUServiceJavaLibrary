package dtu.services.library.config.events;


class Message
{
    private String topic;
    private Object value;

    // Jackson needs a non-null default constructor
    public Message() {}

    public Message(String topic, Object value)
    {
        this.topic = topic;
        this.value = value;
    }

    // Ensure these exist so Jackson can "set" the values
    public String getTopic() { return topic; }
    public Object getValue() { return value; }
}