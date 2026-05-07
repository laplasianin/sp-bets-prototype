package com.example.betting.api.dto;

import com.example.betting.domain.BetStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BetResponse(
        Long id,
        String userId,
        String eventId,
        String marketId,
        String winnerId,
        BigDecimal amount,
        BetStatus status,
        BigDecimal payout,
        LocalDateTime settledAt,
        LocalDateTime createdAt
) {}
