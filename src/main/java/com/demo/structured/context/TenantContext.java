package com.demo.structured.context;

/**
 * Multi-tenant context for SaaS applications.
 *
 * CRITICAL SAFETY: With ThreadLocal, a reused pooled thread can carry the PREVIOUS
 * tenant's data if remove() was missed — a catastrophic cross-tenant data breach.
 * ScopedValue makes this IMPOSSIBLE: binding is syntactically tied to .run()/.call()
 * block boundary. When the scope ends, the tenant context is unreachable. Period.
 */
public record TenantContext(String tenantId, String schemaName, DataTier tier) {

    public static final ScopedValue<TenantContext> CURRENT = ScopedValue.newInstance();

    public enum DataTier { STANDARD, PREMIUM, DEDICATED }

    /** Fail-fast accessor — prevents accidental use outside a tenant-scoped request. */
    public static TenantContext require() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("No tenant context — called outside request scope");
        }
        return CURRENT.get();
    }
}
