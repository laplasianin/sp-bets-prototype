package com.example.betting.messaging;

import com.example.betting.domain.BetSettlement;
import com.example.betting.domain.BetStatus;
import com.example.betting.messaging.rocketmq.mock.MockRocketMqBroker;
import com.example.betting.messaging.rocketmq.mock.RocketMqMockListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class MockRocketMqBrokerTest {

    private MockRocketMqBroker broker;
    private TestListener testListener;

    @BeforeEach
    void setUp() {
        testListener = new TestListener();

        StaticApplicationContext context = new StaticApplicationContext();
        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        factory.registerSingleton("testListener", testListener);
        context.refresh();

        broker = new MockRocketMqBroker(context);
        broker.afterSingletonsInstantiated();
    }

    @AfterEach
    void tearDown() {
        broker.stop();
    }

    @Test
    void enqueue_dispatchesToListener() {
        BetSettlement settlement = new BetSettlement(42L, "evt-test", BetStatus.WON, new BigDecimal("200.00"));

        assertThat(broker.getListenerCount()).isGreaterThan(0);
        assertThat(testListener.callCount).isZero();
        broker.enqueue("bet-settlements", settlement);

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> testListener.callCount > 0);

        assertThat(testListener.lastSettlement).isNotNull();
        assertThat(testListener.lastSettlement.getOutcome()).isEqualTo(BetStatus.WON);
    }

    @Test
    void enqueue_afterMaxRetries_movesToDlq() {
        testListener.shouldThrow = true;
        BetSettlement settlement = new BetSettlement(99L, "evt-test", BetStatus.LOST, BigDecimal.ZERO);
        broker.enqueue("bet-settlements", settlement);

        await().atMost(8, TimeUnit.SECONDS)
                .until(() -> !broker.getDlq().isEmpty());

        assertThat(broker.getDlq()).hasSize(1);
    }

    static class TestListener {
        volatile int callCount = 0;
        volatile BetSettlement lastSettlement = null;
        volatile boolean shouldThrow = false;

        @RocketMqMockListener(topic = "bet-settlements")
        public void handle(BetSettlement settlement) {
            if (shouldThrow) {
                throw new RuntimeException("Simulated failure");
            }
            lastSettlement = settlement;
            callCount++;
        }
    }
}
