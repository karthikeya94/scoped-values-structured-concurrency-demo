package com.demo.structured.auth;

import com.demo.structured.context.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  USE CASE 2 — Authentication & Authorization Context Propagation        ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  PATTERN:   Use Structured Concurrency to fan out enrichment tasks in   ║
 * ║             parallel. AuthContext flows implicitly replacing            ║
 * ║             SecurityContextHolder logic safely.                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
@Service
public class AuthContextService {

    private static final Logger log = LoggerFactory.getLogger(AuthContextService.class);

    public Map<String, Object> createOrder(Map<String, Object> orderRequest) throws Exception {
        var auth = AuthContext.require();

        if (!auth.hasRole("ORDER_WRITE")) {
            throw new SecurityException("Insufficient permissions for user: " + auth.userId());
        }

        log.info("[user:{}] Creating order with parallel tasks", auth.userId());

        try (var scope = StructuredTaskScope.open()) {
            var inventoryTask = scope.fork(() -> validateInventory(orderRequest));
            var creditTask = scope.fork(this::checkCredit);
            var pricingTask = scope.fork(() -> calculatePricing(orderRequest));
            var auditTask = scope.fork(() -> recordAudit("ORDER_CREATED"));

            scope.join();

            return Map.of(
                "userId", auth.userId(),
                "tenantId", auth.tenantId(),
                "inventory", inventoryTask.get(),
                "credit", creditTask.get(),
                "pricing", pricingTask.get(),
                "audit", auditTask.get()
            );
        }
    }

    private String validateInventory(Map<String, Object> request) {
        return "INVENTORY_VALIDATED";
    }

    private String checkCredit() {
        var auth = AuthContext.require();
        log.info("Checking credit for user: {}", auth.userId());
        return "CREDIT_APPROVED";
    }

    private Map<String, String> calculatePricing(Map<String, Object> request) {
        var auth = AuthContext.require();
        log.info("Calculating pricing for tenant: {}", auth.tenantId());
        return Map.of("total", "$459.99", "tier", "PREMIUM");
    }

    private Map<String, String> recordAudit(String operation) {
        var auth = AuthContext.require();
        log.info("[AUDIT] user={} tenant={} op={}", auth.userId(), auth.tenantId(), operation);
        return Map.of(
            "operation", operation,
            "userId", auth.userId(),
            "timestamp", Instant.now().toString()
        );
    }
}
