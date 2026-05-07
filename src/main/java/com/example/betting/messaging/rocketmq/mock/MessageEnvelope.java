package com.example.betting.messaging.rocketmq.mock;

import com.example.betting.domain.BetSettlement;

class MessageEnvelope {
    final String topic;
    final BetSettlement payload;

    MessageEnvelope(String topic, BetSettlement payload) {
        this.topic = topic;
        this.payload = payload;
    }
}
