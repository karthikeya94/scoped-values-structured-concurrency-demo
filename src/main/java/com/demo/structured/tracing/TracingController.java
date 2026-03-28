package com.demo.structured.tracing;

import com.demo.structured.context.TraceContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracing")
public class TracingController {

    private final TracingService tracingService;

    public TracingController(TracingService tracingService) {
        this.tracingService = tracingService;
    }

    /**
     * GET /api/tracing/order/{orderId}
     * Header: X-Trace-ID (optional — auto-generated if absent)
     *
     * Binds trace context at HTTP edge → all parallel subtasks inherit it.
     */
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getOrderBundle(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Trace-ID", required = false) String incomingTraceId) throws Exception {

        var ctx = new TraceContext(
            incomingTraceId != null ? incomingTraceId : UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            null
        );

        // Bind trace context → everything inside inherits it via ScopedValue
        return ScopedValue.where(TraceContext.CURRENT, ctx)
                          .call(() -> tracingService.fetchOrderBundle(orderId));
        // ↑ No cleanup needed — scope ends here automatically
    }
}
