package com.example.betting.messaging.kafka;

import com.example.betting.domain.EventOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventOutcomeProducer {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeProducer.class);

    private final KafkaTemplate<String, EventOutcome> kafkaTemplate;
    private final String topic;

    public EventOutcomeProducer(KafkaTemplate<String, EventOutcome> kafkaTemplate,
                                @Value("${app.topics.event-outcomes}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(EventOutcome outcome) {
        log.info("Publishing event outcome to Kafka: eventId={}", outcome.getEventId());
        kafkaTemplate.send(topic, outcome.getEventId(), outcome)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event outcome: eventId={}", outcome.getEventId(), ex);
                    }
                });
    }
}
