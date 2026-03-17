package dtu.services.library.events;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;


@Component
class QueueListener
{
    private final TopicRouter router;
    private final K8Coordinator coordinator;

    @Value("${service.version}") String version;
    @Value("${spring.application.name}") String service;

    private static final Logger log = LoggerFactory.getLogger(QueueListener.class);



    public QueueListener(TopicRouter router, K8Coordinator coordinator)
    {
        this.router = router;
        this.coordinator = coordinator;
    }


    @KafkaListener
    (
        id = "businessWorker",
        topics = "#{topicRouter.getRegisteredTopics()}",
        groupId = "${spring.application.name}",
        autoStartup = "false"
    )
    public void receiveFromQueue(Message<Object> message)
    {
        router.onMessage(message);
    }


    @KafkaListener
    (
        id = "lifecycle",
        topics = "service.lifecycle.announcements",
        groupId = "#{uniqueInstanceId}"
    )
    public void receiveLifecycleSignal(Message<Map<String,String>> message)
    {
        String service = message.getPayload().get("service");
        String version = message.getPayload().get("version");

        // 1. Safety check: Only react to our own service type
        if (this.service == null || !this.service.equals(service)) {
            return;
        }

        // 2. Only the current leader needs to check if it should step down
        if (coordinator.isLeader() && isNewer(version,this.version))
        {
            log.info("Newer version {} detected. Stepping down...",version);
            coordinator.yieldLeadership();
        }
    }


    private boolean isNewer(String a, String b)
    {
        String[] v1 = a.split("\\.");
        String[] v2 = b.split("\\.");

        for (int i = 0; i < Math.max(v1.length, v2.length); i++)
        {
            if (Integer.parseInt(v1[i]) > Integer.parseInt(v2[i]))
                return(true);
        }

        return(false);
    }
}
