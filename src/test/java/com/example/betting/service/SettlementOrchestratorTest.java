package com.example.betting.service;

import com.example.betting.domain.Bet;
import com.example.betting.domain.BetStatus;
import com.example.betting.domain.EventOutcome;
import com.example.betting.messaging.rocketmq.BetSettlementProducer;
import com.example.betting.persistence.BetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementOrchestratorTest {

    @Mock
    BetRepository betRepository;

    @Mock
    BetSettlementProducer settlementProducer;

    @InjectMocks
    SettlementOrchestrator orchestrator;

    @Test
    void process_sendsSettlementForEachPendingBet() {
        Bet winBet = bet(1L, "evt-1", "team-real", new BigDecimal("100.00"));
        Bet loseBet = bet(2L, "evt-1", "team-barca", new BigDecimal("200.00"));

        when(betRepository.findPendingByEventId("evt-1")).thenReturn(List.of(winBet, loseBet));

        orchestrator.process(new EventOutcome("evt-1", "Real vs Barca", "team-real"));

        verify(settlementProducer, times(2)).send(any());
    }

    @Test
    void process_calculatesPayoutCorrectly() {
        Bet winBet = bet(1L, "evt-1", "team-real", new BigDecimal("100.00"));
        when(betRepository.findPendingByEventId("evt-1")).thenReturn(List.of(winBet));

        var captor = ArgumentCaptor.forClass(com.example.betting.domain.BetSettlement.class);

        orchestrator.process(new EventOutcome("evt-1", "Real vs Barca", "team-real"));

        verify(settlementProducer).send(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(BetStatus.WON);
        assertThat(captor.getValue().getPayout()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void process_setsLostForNonMatchingWinner() {
        Bet loseBet = bet(2L, "evt-1", "team-barca", new BigDecimal("200.00"));
        when(betRepository.findPendingByEventId("evt-1")).thenReturn(List.of(loseBet));

        var captor = ArgumentCaptor.forClass(com.example.betting.domain.BetSettlement.class);

        orchestrator.process(new EventOutcome("evt-1", "Real vs Barca", "team-real"));

        verify(settlementProducer).send(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(BetStatus.LOST);
        assertThat(captor.getValue().getPayout()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void process_doesNothingWhenNoPendingBets() {
        when(betRepository.findPendingByEventId("evt-99")).thenReturn(List.of());
        orchestrator.process(new EventOutcome("evt-99", "No Bets Game", "team-x"));
        verifyNoInteractions(settlementProducer);
    }

    private Bet bet(Long id, String eventId, String winnerId, BigDecimal amount) {
        Bet bet = new Bet();
        bet.setId(id);
        bet.setUserId("user-" + id);
        bet.setEventId(eventId);
        bet.setMarketId("market-1h");
        bet.setWinnerId(winnerId);
        bet.setAmount(amount);
        bet.setStatus(BetStatus.PENDING);
        return bet;
    }
}
