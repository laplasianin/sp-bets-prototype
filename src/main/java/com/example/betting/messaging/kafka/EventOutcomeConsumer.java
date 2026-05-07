package com.example.betting.messaging.kafka;

import com.example.betting.domain.EventOutcome;
import com.example.betting.service.SettlementOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventOutcomeConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeConsumer.class);

    private final SettlementOrchestrator orchestrator;

    public EventOutcomeConsumer(SettlementOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
        topics = "${app.topics.event-outcomes}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(EventOutcome outcome) {
        log.info("Received event outcome from Kafka: eventId={}, winnerId={}", outcome.getEventId(), outcome.getWinnerId());
        orchestrator.process(outcome);
    }
}
