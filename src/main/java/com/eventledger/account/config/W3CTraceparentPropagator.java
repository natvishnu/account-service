package com.eventledger.account.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal W3C Trace Context propagator (the {@code traceparent} header).
 *
 * <p>Supplied explicitly because the OpenTelemetry propagator auto-configured in this Spring
 * Boot / OTel version combination resolves to a no-op (its {@code fields()} is empty), so neither
 * injection nor extraction happens. Registering this as the primary {@link Propagator} makes
 * Micrometer's sender/receiver tracing handlers propagate the trace, so a single request produces
 * one shared trace id across the Gateway and the Account Service.
 */
public class W3CTraceparentPropagator implements Propagator {

    private static final Pattern TRACEPARENT =
            Pattern.compile("^00-([0-9a-fA-F]{32})-([0-9a-fA-F]{16})-([0-9a-fA-F]{2})$");

    private final Tracer tracer;

    public W3CTraceparentPropagator(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public List<String> fields() {
        return List.of("traceparent");
    }

    @Override
    public <C> void inject(TraceContext context, C carrier, Setter<C> setter) {
        if (context == null || carrier == null) {
            return;
        }
        String flags = Boolean.TRUE.equals(context.sampled()) ? "01" : "00";
        String traceparent = "00-" + context.traceId() + "-" + context.spanId() + "-" + flags;
        setter.set(carrier, "traceparent", traceparent);
    }

    @Override
    public <C> Span.Builder extract(C carrier, Getter<C> getter) {
        String traceparent = getter.get(carrier, "traceparent");
        if (traceparent != null) {
            Matcher matcher = TRACEPARENT.matcher(traceparent.trim());
            if (matcher.matches()) {
                boolean sampled = (Integer.parseInt(matcher.group(3), 16) & 0x01) != 0;
                TraceContext parent = tracer.traceContextBuilder()
                        .traceId(matcher.group(1))
                        .spanId(matcher.group(2))
                        .sampled(sampled)
                        .build();
                return tracer.spanBuilder().setParent(parent);
            }
        }
        return tracer.spanBuilder();
    }
}
