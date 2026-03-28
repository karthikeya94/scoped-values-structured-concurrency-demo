package com.demo.structured.experiment;

import com.demo.structured.context.ExperimentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  USE CASE 4 — Feature Flags & A/B Testing via Nested Scope Rebinding    ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  PATTERN:   Parallel processing with nested scope rebinding properties. ║
 * ║             Shows how to shadow configurations for part of a request    ║
 * ║             while processing concurrently.                              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    public Map<String, Object> renderProduct(String productId) throws Exception {
        var exp = ExperimentContext.current();
        log.info("[cohort:{}] Rendering product {}", exp.cohort(), productId);

        try (var scope = StructuredTaskScope.open()) {
            var productViewTask = scope.fork(() -> buildProductView(productId));
            var analyticsTagTask = scope.fork(() -> tagAnalytics(productId));

            scope.join();

            return Map.of(
                "cohort", exp.cohort(),
                "product", productViewTask.get(),
                "analytics", analyticsTagTask.get()
            );
        }
    }

    public Map<String, Object> renderWithAdminOverride(String productId) throws Exception {
        var originalCohort = ExperimentContext.current().cohort();
        log.info("[original cohort:{}] Starting render with admin override", originalCohort);

        var userView = renderProduct(productId);

        var adminView = ScopedValue
            .where(ExperimentContext.CURRENT, ExperimentContext.CONTROL)
            .call(() -> {
                log.info("[shadowed to:{}] Admin preview rendering", ExperimentContext.current().cohort());
                return renderProduct(productId);
            });

        log.info("[restored to:{}] Back to user's cohort", ExperimentContext.current().cohort());

        return Map.of(
            "originalCohort", originalCohort,
            "userView", userView,
            "adminPreview", adminView,
            "restoredCohort", ExperimentContext.current().cohort()
        );
    }

    private Map<String, Object> buildProductView(String productId) {
        var exp = ExperimentContext.current();
        return Map.of(
            "productId", productId,
            "showNewPricingUI", exp.flag("new-pricing-layout"),
            "showRecommendationsV2", exp.flag("recommendations-v2"),
            "cohort", exp.cohort()
        );
    }

    private Map<String, String> tagAnalytics(String productId) {
        var exp = ExperimentContext.current();
        return Map.of(
            "event", "product_view",
            "cohort", exp.cohort(),
            "productId", productId
        );
    }
}
