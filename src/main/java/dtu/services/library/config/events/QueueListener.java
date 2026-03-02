package dtu.services.library.config.events;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;


@Component
class QueueListener
{
    private final TopicRouter topicRouter;
    @Value("${service.version}") String version;
    @Value("${spring.application.name}") String service;


    public QueueListener(TopicRouter router)
    {
        this.topicRouter = router;
    }


    @KafkaListener
    (
        id = "businessWorker",
        topics = "#{topicRouter.getRegisteredTopics()}",
        groupId = "${spring.application.name}"
    )
    public void receiveFromQueue(Message message)
    {
        topicRouter.onMessage(message);
    }


    @KafkaListener(
        id = "lifecycle",
        topics = "service.lifecycle.announcements",
        groupId = "#{uniqueInstanceId}"
    )


    public void receiveLifecycleSignal(Message message)
    {
        System.out.println(message.getValue());
        System.out.println("This service is "+service+" version "+version);
    }
}
