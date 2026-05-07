package com.example.betting.messaging.rocketmq.mock;

import com.example.betting.domain.BetSettlement;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Component
public class MockRocketMqBroker implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(MockRocketMqBroker.class);
    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_MS = 500L;

    private final ApplicationContext context;
    private final BlockingQueue<MessageEnvelope> queue = new LinkedBlockingQueue<>();
    private final BlockingQueue<MessageEnvelope> dlq = new LinkedBlockingQueue<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rocketmq-mock-worker");
        t.setDaemon(false);
        return t;
    });
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "rocketmq-retry-scheduler");
        t.setDaemon(false);
        return t;
    });
    private volatile boolean running = true;

    private final List<ListenerRegistration> listeners = new ArrayList<>();

    public MockRocketMqBroker(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        discoverListeners();
        worker.submit(this::dispatchLoop);
        log.info("MockRocketMqBroker started with {} listener(s)", listeners.size());
    }

    @PreDestroy
    public void stop() {
        log.info("MockRocketMqBroker stopping");
        running = false;
        worker.shutdownNow();
        scheduler.shutdownNow();
    }

    public void enqueue(String topic, BetSettlement settlement) {
        log.info("Enqueueing settlement betId={} topic={}", settlement.getBetId(), topic);
        queue.add(new MessageEnvelope(topic, settlement));
    }

    public List<MessageEnvelope> getDlq() {
        return List.copyOf(dlq);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    private Iterable<String> allBeanNames() {
        Set<String> names = new LinkedHashSet<>(Arrays.asList(context.getBeanDefinitionNames()));
        if (context instanceof ConfigurableApplicationContext cac) {
            names.addAll(Arrays.asList(cac.getBeanFactory().getSingletonNames()));
        }
        return names;
    }

    private void discoverListeners() {
        for (String beanName : allBeanNames()) {
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getMethods()) {
                RocketMqMockListener ann = AnnotationUtils.findAnnotation(method, RocketMqMockListener.class);
                if (ann != null && method.getParameterCount() == 1 && method.getParameterTypes()[0] == BetSettlement.class) {
                    method.setAccessible(true);
                    listeners.add(new ListenerRegistration(ann.topic(), bean, method));
                    log.info("Registered RocketMQ mock listener: {}.{} on topic '{}'",
                            targetClass.getSimpleName(), method.getName(), ann.topic());
                }
            }
        }
    }

    private void dispatchLoop() {
        log.info("MockRocketMqBroker dispatch loop started, running={}", running);
        while (running) {
            try {
                MessageEnvelope envelope = queue.poll(200, TimeUnit.MILLISECONDS);
                if (envelope != null) {
                    log.info("Dequeued settlement betId={}", envelope.payload.getBetId());
                    dispatch(envelope);
                }
            } catch (InterruptedException e) {
                log.info("Dispatch loop interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error in dispatch loop", e);
            }
        }
        log.info("MockRocketMqBroker dispatch loop exited, running={}", running);
    }

    private void dispatch(MessageEnvelope envelope) {
        for (ListenerRegistration reg : listeners) {
            if (!reg.topic().equals(envelope.topic)) continue;
            dispatchToListener(envelope.payload, reg, 0);
        }
    }

    private void dispatchToListener(BetSettlement payload, ListenerRegistration reg, int attempt) {
        try {
            log.debug("Dispatching to {}.{} attempt={}", reg.bean().getClass().getSimpleName(), reg.method().getName(), attempt);
            reg.method().invoke(reg.bean(), payload);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (attempt < MAX_RETRIES) {
                long delay = BACKOFF_MS * (1L << attempt);
                log.warn("Listener {}.{} failed (attempt {}), retrying in {}ms: {}",
                        reg.bean().getClass().getSimpleName(), reg.method().getName(),
                        attempt + 1, delay, cause.getMessage());
                scheduler.schedule(() -> dispatchToListener(payload, reg, attempt + 1), delay, TimeUnit.MILLISECONDS);
            } else {
                log.error("Message exceeded max retries, moving to DLQ: betId={}", payload.getBetId());
                dlq.add(new MessageEnvelope(reg.topic(), payload));
            }
        }
    }

    private record ListenerRegistration(String topic, Object bean, Method method) {}
}
