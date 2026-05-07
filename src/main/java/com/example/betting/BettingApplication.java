package com.example.betting;

import com.example.betting.config.EmbeddedKafkaInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BettingApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BettingApplication.class);
        app.addInitializers(new EmbeddedKafkaInitializer());
        app.run(args);
    }
}
