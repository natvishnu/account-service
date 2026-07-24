package com.eventledger.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Injectable clock so time-dependent logic stays testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
