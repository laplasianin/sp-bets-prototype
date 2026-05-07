package com.example.betting.service;

import com.example.betting.domain.Bet;
import com.example.betting.domain.BetSettlement;
import com.example.betting.domain.BetStatus;
import com.example.betting.domain.EventOutcome;
import com.example.betting.messaging.rocketmq.BetSettlementProducer;
import com.example.betting.persistence.BetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SettlementOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SettlementOrchestrator.class);
    private static final BigDecimal WIN_MULTIPLIER = BigDecimal.valueOf(2);

    private final BetRepository betRepository;
    private final BetSettlementProducer settlementProducer;

    public SettlementOrchestrator(BetRepository betRepository, BetSettlementProducer settlementProducer) {
        this.betRepository = betRepository;
        this.settlementProducer = settlementProducer;
    }

    public void process(EventOutcome outcome) {
        List<Bet> pendingBets = betRepository.findPendingByEventId(outcome.getEventId());
        log.info("Processing outcome for eventId={}: {} pending bets found", outcome.getEventId(), pendingBets.size());

        for (Bet bet : pendingBets) {
            BetSettlement settlement = buildSettlement(bet, outcome);
            settlementProducer.send(settlement);
        }
    }

    private BetSettlement buildSettlement(Bet bet, EventOutcome outcome) {
        boolean won = bet.getWinnerId().equals(outcome.getWinnerId());
        BetStatus status = won ? BetStatus.WON : BetStatus.LOST;
        BigDecimal payout = won ? bet.getAmount().multiply(WIN_MULTIPLIER) : BigDecimal.ZERO;
        return new BetSettlement(bet.getId(), bet.getEventId(), status, payout);
    }
}
