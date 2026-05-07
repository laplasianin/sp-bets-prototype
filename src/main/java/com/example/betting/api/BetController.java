package com.example.betting.api;

import com.example.betting.api.dto.BetResponse;
import com.example.betting.mapper.BetMapper;
import com.example.betting.persistence.BetRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BetController {

    private final BetRepository betRepository;
    private final BetMapper betMapper;

    public BetController(BetRepository betRepository, BetMapper betMapper) {
        this.betRepository = betRepository;
        this.betMapper = betMapper;
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
