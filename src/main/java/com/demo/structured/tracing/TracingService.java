package com.demo.structured.tracing;

import com.demo.structured.context.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  USE CASE 1 — Distributed Tracing Context Propagation                   ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  PATTERN:   Bind traceId + spanId once at HTTP entry.                   ║
 * ║             Every forked thread nested in the request automatically     ║
 * ║             inherits the context. Zero boilerplate or parameter         ║
 * ║             passing required.                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
@Service
public class TracingService {

    private static final Logger log = LoggerFactory.getLogger(TracingService.class);

    public Map<String, Object> fetchOrderBundle(String orderId) throws Exception {
        var ctx = TraceContext.current();   // ← now correct
        log.info("[trace:{}][span:{}] Starting parallel order bundle fetch", ctx.traceId(), ctx.spanId());

        try (var scope = StructuredTaskScope.open()) {
            var orderTask = scope.fork(() -> fetchOrder(orderId));
            var inventoryTask = scope.fork(() -> checkInventory(orderId));
            var pricingTask = scope.fork(() -> getPrice(orderId));

            scope.join();

            return Map.of(
                    "traceId", ctx.traceId(),
                    "spanId", ctx.spanId(),
                    "order", orderTask.get(),
                    "inventory", inventoryTask.get(),
                    "pricing", pricingTask.get()
            );
        }
    }

    private Map<String, String> fetchOrder(String orderId) {
        var ctx = TraceContext.require();
        log.info("[trace:{}] Fetching order {}", ctx.traceId(), orderId);
        return Map.of("orderId", orderId, "status", "CONFIRMED", "amount", "$299.99");
    }

    private Map<String, String> checkInventory(String orderId) {
        var ctx = TraceContext.require();
        log.info("[trace:{}] Checking inventory for {}", ctx.traceId(), orderId);
        return Map.of("orderId", orderId, "available", "true", "warehouse", "WH-EAST");
    }

    private Map<String, String> getPrice(String orderId) {
        var ctx = TraceContext.require();
        log.info("[trace:{}] Calculating price for {}", ctx.traceId(), orderId);
        return Map.of("orderId", orderId, "basePrice", "$249.99", "finalPrice", "$199.99");
    }
}
