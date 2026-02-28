package dtu.services.library.config;

import java.util.Map;
import org.springframework.kafka.core.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.web.client.RestClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import dtu.services.library.config.events.DTUEvents;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;


@EnableKafka
@Configuration
@Component("QueueBean")

class Kafka
{
    private final RestClient client;
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());


    public Kafka(RestClient client)
    {
        this.client = client;
    }


    @SuppressWarnings("unchecked")
    private Map<String,Object> fetchKafkaConfig()
    {
        String url = Environment.QUEUE_URL;
        String yaml = client.get().uri(url).retrieve().body(String.class);
        Map<String, Object> response = mapper.readValue(yaml, Map.class);
        return((Map<String,Object>) response.get("queue"));
    }


    @Bean(name = "uniqueInstanceId")
    public String uniqueInstanceId()
    {
        return java.util.UUID.randomUUID().toString();
    }


    @Bean
    public ProducerFactory<String,Object> producerFactory()
    {
        return(new DefaultKafkaProducerFactory<>(fetchKafkaConfig()));
    }


    @Bean
    public KafkaTemplate<String,Object> kafkaTemplate()
    {
        return(new KafkaTemplate<>(producerFactory()));
    }


    @Bean
    public ConsumerFactory<String,Object> consumerFactory()
    {
        return(new DefaultKafkaConsumerFactory<>(fetchKafkaConfig()));
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,Object> kafkaListenerContainerFactory()
    {
        ConcurrentKafkaListenerContainerFactory<String,Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return(factory);
    }


    @Bean
    public ApplicationRunner sendStartupSignal
    (
        DTUEvents events,
        @Value("${service.version}") String version,
        @Value("${spring.application.name}") String service
    )
    {
        Map<String, String> payload = Map.of("service_name", service,"version", version);
        return(args -> {events.publish("service.lifecycle.announcements", payload);});
    }


    @Bean(name = "applicationName")
    public String applicationName()
    {
        return((String) fetchKafkaConfig().get("spring.application.name"));
    }
}