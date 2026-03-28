package com.demo.structured.tenant;

import com.demo.structured.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
public class MultiTenantController {

    private final MultiTenantService multiTenantService;

    public MultiTenantController(MultiTenantService multiTenantService) {
        this.multiTenantService = multiTenantService;
    }

    /**
     * GET /api/tenant/dashboard?from=2026-01-01&to=2026-03-28
     * Header: X-Tenant-ID (identifies the tenant)
     *
     * Demonstrates: Tenant isolation via ScopedValue. Parallel analytics queries
     * automatically target the correct schema. No ThreadLocal leak possible.
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard(
            @RequestParam(defaultValue = "2026-01-01") String from,
            @RequestParam(defaultValue = "2026-03-28") String to,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "acme") String tenantId)
            throws Exception {

        // Resolve tenant (simulated registry lookup)
        var tenant = new TenantContext(
            tenantId,
            "schema_" + tenantId,
            TenantContext.DataTier.PREMIUM
        );

        // Bind tenant → every parallel query inside routes to this tenant's schema
        return ScopedValue.where(TenantContext.CURRENT, tenant)
                          .call(() -> multiTenantService.generateDashboard(from, to));
        // ↑ tenant context is unreachable after this returns — zero-leak guarantee
    }
}
