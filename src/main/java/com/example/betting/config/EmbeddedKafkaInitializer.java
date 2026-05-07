package com.example.betting.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.util.Map;

public class EmbeddedKafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (!applicationContext.getEnvironment().matchesProfiles("railway")) {
            return;
        }
        EmbeddedKafkaBroker broker = new EmbeddedKafkaKraftBroker(1, 1, "event-outcomes", "event-outcomes.DLT");
        try {
            broker.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded Kafka broker", e);
        }
        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("embedded-kafka", Map.of(
                        "spring.kafka.bootstrap-servers", broker.getBrokersAsString()
                )));
        Runtime.getRuntime().addShutdownHook(new Thread(broker::destroy));
    }
}
