package com.example.betting.messaging.rocketmq.mock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RocketMqMockListener {
    String topic();
}
