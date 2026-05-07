package com.example.betting.mapper;

import com.example.betting.api.dto.EventOutcomeRequest;
import com.example.betting.domain.EventOutcome;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventOutcomeMapper {
    EventOutcome toDomain(EventOutcomeRequest request);
}
