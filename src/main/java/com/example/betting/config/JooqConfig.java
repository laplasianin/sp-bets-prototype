package com.example.betting.config;

import org.jooq.conf.RenderTable;
import org.jooq.conf.Settings;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {

    @Bean
    public DefaultConfigurationCustomizer jooqCustomizer() {
        return config -> config.set(new Settings().withRenderTable(RenderTable.NEVER));
    }
}
