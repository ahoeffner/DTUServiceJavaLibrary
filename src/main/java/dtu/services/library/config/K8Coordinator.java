package dtu.services.library.config;

import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;


@Component
public class K8Coordinator
{
    private boolean leader = false;

    @EventListener(OnGrantedEvent.class)
    public void onLeadershipGranted(OnGrantedEvent event)
    {
        this.leader = true;
    }

    @EventListener(OnRevokedEvent.class)
    public void onLeadershipRevoked(OnRevokedEvent event)
    {
        this.leader = false;
    }
}