package com.example.betting.api.dto;

import java.time.Instant;

public record EventOutcomeAcceptedResponse(String eventId, Instant acceptedAt) {}
