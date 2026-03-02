package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import java.util.HashMap;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import dtu.services.library.config.events.DTUEvents;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;


@EnableKafka
@Configuration
@Component("QueueBean")

class Kafka
{
    private static final Logger log = LoggerFactory.getLogger(Kafka.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());


    @SuppressWarnings("unchecked")
    private Map<String,Object> fetchKafkaConfig()
    {
        try
        {
            InputStream is = new ClassPathResource("kafka.yaml").getInputStream();
            Map<String, Object> config = mapper.readValue(is, Map.class);

            config = (Map<String,Object>) config.get("kafka");
            config = (Map<String,Object>) config.get(Environment.TYPE);

            return(config);
        }
        catch (Exception e)
        {
            log.error("Unable to load kafka settings",e);
            return(new HashMap<String,Object>());
        }
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