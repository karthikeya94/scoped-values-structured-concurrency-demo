package com.demo.structured.tenant;

import com.demo.structured.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  USE CASE 3 — Multi-Tenant Context Isolation                            ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  PATTERN:   Parallel tenant-specific sub-queries routed safely.        ║
 * ║             ScopedValue prevents cross-tenant data leaks completely.    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
@Service
public class MultiTenantService {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantService.class);

    public Map<String, Object> generateDashboard(String dateFrom, String dateTo) throws Exception {
        var tenant = TenantContext.require();
        log.info("[tenant:{}] Generating dashboard on schema {}", tenant.tenantId(), tenant.schemaName());

        try (var scope = StructuredTaskScope.open()) {
            var salesTask = scope.fork(() -> querySales(dateFrom, dateTo));
            var usageTask = scope.fork(() -> queryUsage(dateFrom, dateTo));
            var billingTask = scope.fork(() -> queryBilling(dateFrom, dateTo));

            scope.join();

            return Map.of(
                "tenantId", tenant.tenantId(),
                "schema", tenant.schemaName(),
                "tier", tenant.tier().name(),
                "sales", salesTask.get(),
                "usage", usageTask.get(),
                "billing", billingTask.get()
            );
        }
    }

    private Map<String, Object> querySales(String from, String to) {
        var tenant = TenantContext.require();
        log.info("[{}] SELECT * FROM {}.sales WHERE date BETWEEN ...", tenant.tenantId(), tenant.schemaName());
        return Map.of("schema", tenant.schemaName(), "totalSales", 42500, "orders", 128);
    }

    private Map<String, Object> queryUsage(String from, String to) {
        var tenant = TenantContext.require();
        log.info("[{}] SELECT * FROM {}.usage_metrics ...", tenant.tenantId(), tenant.schemaName());
        return Map.of("schema", tenant.schemaName(), "apiCalls", 985000, "storageGB", 12.5);
    }

    private Map<String, Object> queryBilling(String from, String to) {
        var tenant = TenantContext.require();
        log.info("[{}] SELECT * FROM {}.billing_records ...", tenant.tenantId(), tenant.schemaName());
        return Map.of("schema", tenant.schemaName(), "totalBilled", "$8,750.00", "status", "PAID");
    }
}
