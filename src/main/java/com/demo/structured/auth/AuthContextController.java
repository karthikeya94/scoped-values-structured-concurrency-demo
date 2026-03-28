package com.demo.structured.auth;

import com.demo.structured.context.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthContextController {

    private final AuthContextService authContextService;

    public AuthContextController(AuthContextService authContextService) {
        this.authContextService = authContextService;
    }

    /**
     * POST /api/auth/create-order
     * Headers: X-User-ID, X-Tenant-ID (simulates JWT-extracted auth context)
     * Body:    { "item": "LAPTOP-PRO", "quantity": 1 }
     *
     * Binds auth context → parallel enrichment tasks inherit it for
     * permission checks, credit checks, and audit logging.
     */
    @PostMapping("/create-order")
    public Map<String, Object> createOrder(
            @RequestBody Map<String, Object> orderRequest,
            @RequestHeader(value = "X-User-ID", defaultValue = "user-42") String userId,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "acme-corp") String tenantId)
            throws Exception {

        // Simulate JWT validation → build immutable auth context
        var auth = new AuthContext(
            userId, userId + "@example.com",
            Set.of("ORDER_READ", "ORDER_WRITE", "ADMIN"),
            tenantId,
            Instant.now().plusSeconds(3600)
        );

        // Bind auth → every forked task inside sees this same immutable AuthContext
        return ScopedValue.where(AuthContext.CURRENT, auth)
                          .call(() -> authContextService.createOrder(orderRequest));
    }
}
