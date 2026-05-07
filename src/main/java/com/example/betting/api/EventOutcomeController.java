package com.example.betting.api;

import com.example.betting.api.dto.EventOutcomeAcceptedResponse;
import com.example.betting.api.dto.EventOutcomeRequest;
import com.example.betting.mapper.EventOutcomeMapper;
import com.example.betting.messaging.kafka.EventOutcomeProducer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class EventOutcomeController {

    private final EventOutcomeProducer producer;
    private final EventOutcomeMapper outcomeMapper;

    public EventOutcomeController(EventOutcomeProducer producer, EventOutcomeMapper outcomeMapper) {
        this.producer = producer;
        this.outcomeMapper = outcomeMapper;
    }

    @PostMapping("/event-outcomes")
    public ResponseEntity<EventOutcomeAcceptedResponse> publishOutcome(@Valid @RequestBody EventOutcomeRequest request) {
        producer.publish(outcomeMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{eventId}")
                .buildAndExpand(request.eventId())
                .toUri();
        return ResponseEntity.accepted()
                .location(location)
                .body(new EventOutcomeAcceptedResponse(request.eventId(), Instant.now()));
    }
}
