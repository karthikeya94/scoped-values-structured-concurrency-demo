package com.demo.structured.context;

import java.util.Map;

/**
 * A/B testing and feature flag context with nested rebinding support.
 *
 * SHADOWING PATTERN: ScopedValue supports re-binding in nested scopes.
 *   - Inner binding shadows the outer one.
 *   - When the inner scope exits, the outer binding is automatically restored.
 *   - This is IMPOSSIBLE to do safely with ThreadLocal (requires manual save/restore).
 */
public record ExperimentContext(String cohort, Map<String, Boolean> flags) {

    public static final ScopedValue<ExperimentContext> CURRENT = ScopedValue.newInstance();

    public static final ExperimentContext CONTROL =
            new ExperimentContext("control", Map.of());

    /** Check if a feature flag is enabled for this experiment cohort. */
    public boolean flag(String name) {
        return flags.getOrDefault(name, false);
    }

    /** Safe accessor with default fallback to control group. */
    public static ExperimentContext current() {
        return CURRENT.orElse(CONTROL);
    }
}
