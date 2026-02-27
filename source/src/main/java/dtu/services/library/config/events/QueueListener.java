package dtu.services.library.config.events;

import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;


@Component
class QueueListener
{
    private final TopicRouter topicRouter;


    public QueueListener(TopicRouter router)
    {
        this.topicRouter = router;
    }


    @KafkaListener
    (
        topics = "#{topicRouter.getRegisteredTopics()}",
        groupId = "${spring.application.name}"
    )

    public void receiveFromQueue(Message message)
    {
        topicRouter.onMessage(message);
    }
}
