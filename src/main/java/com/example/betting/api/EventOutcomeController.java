package com.example.betting.api;

import com.example.betting.api.dto.BetResponse;
import com.example.betting.api.dto.EventOutcomeAcceptedResponse;
import com.example.betting.api.dto.EventOutcomeRequest;
import com.example.betting.mapper.BetMapper;
import com.example.betting.mapper.EventOutcomeMapper;
import com.example.betting.messaging.kafka.EventOutcomeProducer;
import com.example.betting.persistence.BetRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EventOutcomeController {

    private final EventOutcomeProducer producer;
    private final EventOutcomeMapper outcomeMapper;
    private final BetRepository betRepository;
    private final BetMapper betMapper;

    public EventOutcomeController(EventOutcomeProducer producer,
                                  EventOutcomeMapper outcomeMapper,
                                  BetRepository betRepository,
                                  BetMapper betMapper) {
        this.producer = producer;
        this.outcomeMapper = outcomeMapper;
        this.betRepository = betRepository;
        this.betMapper = betMapper;
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

    @GetMapping("/bets")
    public List<BetResponse> getAllBets() {
        return betMapper.toResponseList(betRepository.findAll());
    }

    @GetMapping("/bets/{id}")
    public ResponseEntity<BetResponse> getBet(@PathVariable Long id) {
        return betRepository.findById(id)
                .map(betMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
