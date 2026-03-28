package com.demo.structured.threadlocal;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ThreadLocalService {

    private static final Logger log = LoggerFactory.getLogger(ThreadLocalService.class);

    /**
     * 1. TRACING PATTERN PROBLEM: Native ThreadLocal Leak.
     * The Controller set this traceID natively. If it doesn't clear it, 
     * this Tomcat thread retains it for the next user.
     */
    public String checkCurrentTrace() {
        String currentTrace = ThreadLocalContext.TRACE_ID.get();
        return "Service executing with trace ID: " + currentTrace;
    }

    /**
     * 1.b. LOG4J THREAD CONTEXT PROBLEM: Widely used in industry but uses ThreadLocals under the hood!
     * Log4J ThreadContext (MDC) suffers from EXACTLY the same leakage and loss-of-context 
     * issues when bouncing between threads if not manually transferred and cleared!
     */
    public String demonstrateLog4jThreadContextContextLoss() throws Exception {
        // Assume controller set this: ThreadContext.put("X-Session-ID", "log4j-session-99");
        
        // Simulating @Async or passing to an executor pool
        CompletableFuture<String> asyncTask = CompletableFuture.supplyAsync(() -> {
            String inheritedSession = ThreadContext.get("X-Session-ID");
            if (inheritedSession == null) {
                return "ERROR: Log4j ThreadContext was LOST in the async thread!";
            }
            return "SUCCESS: Found Log4j context - " + inheritedSession;
        });

        return asyncTask.get();
    }

    /**
     * 2. TENANT PATTERN PROBLEM: Loss of Context in Async Subtasks.
     * Before Structured Concurrency, if we spin up a background thread (e.g., CompletableFuture),
     * plain ThreadLocal is NOT inherited by the new thread.
     */
    public String demonstrateTenantLoss() throws Exception {
        CompletableFuture<String> asyncWorker = CompletableFuture.supplyAsync(() -> {
            String inheritedTenant = ThreadLocalContext.TENANT_ID.get();
            if (inheritedTenant == null) {
                return "ERROR: Plain ThreadLocal Tenant context was NULL in the async worker! It did not inherit.";
            }
            return "SUCCESS: Inherited Tenant - " + inheritedTenant;
        });
        
        return asyncWorker.get();
    }

    /**
     * 3. AUTH PATTERN PROBLEM: Privilege Escalation (Mutability).
     * Since Auth uses InheritableThreadLocal (which attempts to solve the Tenant loss problem),
     * standard Java threads actually share the EXACT same object reference!
     */
    public String demonstrateAuthMutation(String originalRole) throws Exception {
        // Manually creating a thread where InheritableThreadLocal will pass references to it
        CompletableFuture<String> maliciousWorker = CompletableFuture.supplyAsync(() -> {
            ThreadLocalContext.AuthData auth = ThreadLocalContext.AUTH_DATA.get();
            if (auth != null) {
                // Changing role inside the background thread!
                auth.setRole("SUPER_ADMIN");
            }
            return "Malicious async task successfully spoofed permissions.";
        });
        
        maliciousWorker.get(); // Wait for it to finish
        
        // Check the Controller's theoretically "safe" auth reference
        String finalRole = ThreadLocalContext.AUTH_DATA.get().getRole();
        return "Started as: " + originalRole + " | But parent ended up as: " + finalRole + " -> PRIVILEGE ESCALATION LEAK!";
    }

    /**
     * 4. EXPERIMENT PATTERN PROBLEM: Shared Collection Corruption.
     * Even if we pass it around asynchronously to speed up processing, 
     * shared mutable collections in InheritableThreadLocal lead to unpredictable bugs.
     */
    public String demonstrateExperimentCorruption() throws Exception {
        CompletableFuture<String> backgroundTask = CompletableFuture.supplyAsync(() -> {
            List<String> flags = ThreadLocalContext.EXPERIMENT_FLAGS.get();
            if (flags != null) {
                // Subtask corrupts parent's experiment list!
                flags.add("BETA_FEATURE_ENABLED_BY_CHILD");
            }
            return "Child successfully injected hidden feature flag.";
        });

        backgroundTask.get();

        List<String> parentFlags = ThreadLocalContext.EXPERIMENT_FLAGS.get();
        return "Parent's Experiment List After Async Task: " + parentFlags + " (CORRUPTED BY ASYNC TASK)";
    }
}
