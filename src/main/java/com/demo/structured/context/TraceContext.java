package com.demo.structured.context;

import java.util.UUID;

public record TraceContext(String traceId, String spanId, String parentSpanId) {

    public static final ScopedValue<TraceContext> CURRENT = ScopedValue.newInstance();

    public static final TraceContext NOOP = new TraceContext("no-trace", "no-span", null);

    public TraceContext newChildSpan() {
        return new TraceContext(traceId, UUID.randomUUID().toString(), spanId);
    }

    /** Fail-fast accessor — throws if called outside a trace scope. */
    public static TraceContext require() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("No trace context in scope");
        }
        return CURRENT.get();
    }

    /** Safe accessor — never throws (returns NOOP if unbound). */
    public static TraceContext current() {
        return CURRENT.orElse(NOOP);
    }
}