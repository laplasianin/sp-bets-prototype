package com.example.betting.api.dto;

import jakarta.validation.constraints.NotBlank;

public record EventOutcomeRequest(
        @NotBlank String eventId,
        @NotBlank String eventName,
        @NotBlank String winnerId
) {}
