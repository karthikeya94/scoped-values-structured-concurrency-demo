package com.demo.structured.threadlocal;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/threadlocal")
public class ThreadLocalController {

    private final ThreadLocalService threadLocalService;

    public ThreadLocalController(ThreadLocalService threadLocalService) {
        this.threadLocalService = threadLocalService;
    }

    /**
     * Call this multiple times to see the trace ID leaking across Tomcat threads
     * because of an intentional missing clean-up.
     */
    @GetMapping("/trace/leak")
    public Map<String, String> demoTraceLeak(@RequestParam String traceId) {
        String existingLeak = ThreadLocalContext.TRACE_ID.get();
        String leakMessage = (existingLeak != null) ? "WARNING ENCOUNTERED EXISTING LEAK ON THIS THREAD: " + existingLeak : "CLEAN THREAD.";
        
        // Simulating WebFilter or HandlerInterceptor putting context...
        ThreadLocalContext.TRACE_ID.set(traceId);
        
        // Notice we explicitly do not wrap this inside `try/finally` with `remove()`.
        // Refresh this endpoint rapidly on a server to trigger Tomcat's pooled-thread leaks!
        return Map.of(
            "pattern", "Tracing Context Leak via Tomcat pool",
            "leak_status_before_set", leakMessage,
            "result", threadLocalService.checkCurrentTrace()
        );
    }

    /**
     * Demonstrates that Log4j ThreadContext (MDC) which represents industry-standard trace 
     * logging fails identically. If a parent spawns an async thread, MDC metadata vanishes!
     */
    @GetMapping("/log4j/loss")
    public Map<String, String> demoLog4jContextLoss(@RequestParam String sessionId) throws Exception {
        // Log4j implicitly assigns to a proprietary `InheritableThreadLocal` / `ThreadLocal` layer
        ThreadContext.put("X-Session-ID", sessionId);
        
        try {
            return Map.of(
                "pattern", "Log4j ThreadContext Context Loss",
                "result", threadLocalService.demonstrateLog4jThreadContextContextLoss()
            );
        } finally {
            ThreadContext.clearAll(); // Manual explicit wipe requirement
        }
    }

    /**
     * Demonstrates how Plain ThreadLocal completely fails to propagate
     * the tenant context into Virtual Threads / StructuredTaskScope child forks.
     */
    @GetMapping("/tenant/loss")
    public Map<String, String> demoTenantLoss(@RequestParam String tenantId) throws Exception {
        // Controller sets the context
        ThreadLocalContext.TENANT_ID.set(tenantId);
        try {
            return Map.of(
                "pattern", "Tenant Context Loss",
                "result", threadLocalService.demonstrateTenantLoss()
            );
        } finally {
            ThreadLocalContext.TENANT_ID.remove(); 
        }
    }

    /**
     * Demonstrates privilege escalation because InheritableThreadLocal shares pointing 
     * references, allowing child tasks to mutate parent contexts invisibly.
     */
    @GetMapping("/auth/mutation")
    public Map<String, String> demoAuthMutation(@RequestParam String user, @RequestParam String role) throws Exception {
        // Controller statically binds context
        ThreadLocalContext.AUTH_DATA.set(new ThreadLocalContext.AuthData(user, role));
        try {
            return Map.of(
                "pattern", "Auth Context Mutability Leak",
                "result", threadLocalService.demonstrateAuthMutation(role)
            );
        } finally {
            ThreadLocalContext.AUTH_DATA.remove();
        }
    }

    /**
     * Demonstrates experiment state corruption via shared mutable lists in InheritableThreadLocal.
     */
    @GetMapping("/experiment/corruption")
    public Map<String, String> demoExperimentCorruption(@RequestParam List<String> flags) throws Exception {
        // Controller injects the original collection
        ThreadLocalContext.EXPERIMENT_FLAGS.set(new ArrayList<>(flags));
        try {
            return Map.of(
                "pattern", "Experiment Context Data Corruption",
                "result", threadLocalService.demonstrateExperimentCorruption()
            );
        } finally {
            ThreadLocalContext.EXPERIMENT_FLAGS.remove();
        }
    }
}
