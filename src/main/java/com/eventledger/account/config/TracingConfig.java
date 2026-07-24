package com.eventledger.account.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TracingConfig {

    /** Primary so Micrometer's tracing handlers use it instead of the no-op OTel propagator. */
    @Bean
    @Primary
    public Propagator w3cPropagator(Tracer tracer) {
        return new W3CTraceparentPropagator(tracer);
    }
}
