package com.example.betting.persistence;

import com.example.betting.domain.Bet;
import com.example.betting.domain.BetStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BetRepository {
    List<Bet> findPendingByEventId(String eventId);
    Optional<Bet> findById(Long id);
    List<Bet> findAll();
    void settle(Long betId, BetStatus status, BigDecimal payout);
}
