package dtu.services.library.config.events;

import java.util.Map;
import org.slf4j.Logger;
import java.util.HashMap;
import org.slf4j.LoggerFactory;
import dtu.services.library.utils.Reflection;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.integration.leader.Context;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;


@Component
class K8Coordinator
{
    private Context context = null;
    private boolean leader = false;

    @Value("${service.version}")private String version;
    @Value("${spring.application.name}") private String service;

    private final String env;
    private final KafkaTemplate<String,Object> kafka;
    private final KafkaListenerEndpointRegistry registry;

    private static final Logger log = LoggerFactory.getLogger(K8Coordinator.class);



    public K8Coordinator(KafkaTemplate<String,Object> kafka, KafkaListenerEndpointRegistry registry)
    {
        String env = "dev";

        this.kafka = kafka;
        this.registry = registry;

        try {env = (String) Reflection.getStaticField("dtu.services.library.config.Environment","TYPE");}
        catch (Exception e) {log.error("Cannot access environment",e);}

        this.env = env;
    }


    public boolean isLeader()
    {
        return(this.leader);
    }


    @EventListener(OnGrantedEvent.class)
    public void onLeadershipGranted(OnGrantedEvent event)
    {
        this.context = event.getContext();
        MessageListenerContainer container = registry.getListenerContainer("businessWorker");
        if (container != null && !container.isRunning()) container.start();
        this.leader = true;
    }


    @EventListener(OnRevokedEvent.class)
    public void onLeadershipRevoked(OnRevokedEvent event)
    {
        this.leader = false;
        MessageListenerContainer container = registry.getListenerContainer("businessWorker");
        if (container != null) container.stop();
    }


    public void yieldLeadership()
    {
        if (this.context != null) this.context.yield();
    }


    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onStartup()
    {
        if ("dev".equals(this.env))
        {
            this.leader = true;

            MessageListenerContainer container = registry.getListenerContainer("businessWorker");
            if (container != null) container.start();
        }

        Map<String, String> message = new HashMap<>();
        message.put("service", this.service);
        message.put("version", this.version);

        kafka.send("service.lifecycle.announcements", message);
    }
}