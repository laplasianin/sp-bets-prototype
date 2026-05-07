package com.example.betting.service;

import com.example.betting.domain.BetSettlement;
import com.example.betting.messaging.rocketmq.mock.RocketMqMockListener;
import com.example.betting.persistence.BetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BetSettlementHandler {

    private static final Logger log = LoggerFactory.getLogger(BetSettlementHandler.class);

    private final BetRepository betRepository;

    public BetSettlementHandler(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @RocketMqMockListener(topic = "bet-settlements")
    public void handle(BetSettlement settlement) {
        log.info("Settling betId={} outcome={} payout={}", settlement.getBetId(), settlement.getOutcome(), settlement.getPayout());
        betRepository.settle(settlement.getBetId(), settlement.getOutcome(), settlement.getPayout());
    }
}
