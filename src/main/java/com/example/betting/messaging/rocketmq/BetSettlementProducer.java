package com.example.betting.messaging.rocketmq;

import com.example.betting.domain.BetSettlement;

public interface BetSettlementProducer {
    void send(BetSettlement settlement);
}
