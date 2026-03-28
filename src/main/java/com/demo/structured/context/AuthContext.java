package com.demo.structured.context;

import java.time.Instant;
import java.util.Set;

/**
 * Immutable authentication context — replaces ThreadLocal-backed SecurityContextHolder.
 *
 * With ThreadLocal + virtual threads, forked tasks see null (ThreadLocal doesn't inherit).
 * With ScopedValue, every scope.fork() child automatically sees the same AuthContext.
 * Immutable by design — no accidental .set() mutations, no race conditions.
 */
public record AuthContext(
        String userId,
        String email,
        Set<String> roles,
        String tenantId,
        Instant tokenExpiry
) {
    public static final ScopedValue<AuthContext> CURRENT = ScopedValue.newInstance();

    /** Role check — used for authorization in forked subtasks. */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(tokenExpiry);
    }

    /** Fail-fast accessor — throws if called outside an authenticated scope. */
    public static AuthContext require() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("No auth context in scope — called outside authenticated request");
        }
        return CURRENT.get();
    }
}
