package com.example.betting.mapper;

import com.example.betting.api.dto.BetResponse;
import com.example.betting.domain.Bet;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BetMapper {
    BetResponse toResponse(Bet bet);
    List<BetResponse> toResponseList(List<Bet> bets);
}
