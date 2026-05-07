package com.example.betting.messaging.rocketmq.mock;

import com.example.betting.domain.BetSettlement;
import com.example.betting.messaging.rocketmq.BetSettlementProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockRocketMqProducer implements BetSettlementProducer {

    private static final Logger log = LoggerFactory.getLogger(MockRocketMqProducer.class);

    private final MockRocketMqBroker broker;

    public MockRocketMqProducer(MockRocketMqBroker broker) {
        this.broker = broker;
    }

    private static final String TOPIC = "bet-settlements";

    @Override
    public void send(BetSettlement settlement) {
        log.info("Sending to mock RocketMQ {}: betId={}, outcome={}, payout={}",
                TOPIC, settlement.getBetId(), settlement.getOutcome(), settlement.getPayout());
        broker.enqueue(TOPIC, settlement);
    }
}
