package com.demo.structured.experiment;

import com.demo.structured.context.ExperimentContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/experiment")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    /**
     * GET /api/experiment/product/{productId}?cohort=treatment_a
     *
     * Assigns user to experiment cohort and renders product with feature flags.
     * Flags flow through parallel subtasks via ScopedValue.
     */
    @GetMapping("/product/{productId}")
    public Map<String, Object> getProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "treatment_a") String cohort) throws Exception {

        var experimentCtx = new ExperimentContext(cohort, Map.of(
            "new-pricing-layout", true,
            "recommendations-v2", cohort.contains("treatment")
        ));

        return ScopedValue.where(ExperimentContext.CURRENT, experimentCtx)
                          .call(() -> featureFlagService.renderProduct(productId));
    }

    /**
     * GET /api/experiment/product/{productId}/admin-override?cohort=treatment_b
     *
     * Demonstrates NESTED REBINDING: renders user view (assigned cohort)
     * then admin preview (forced control) → original cohort auto-restored.
     */
    @GetMapping("/product/{productId}/admin-override")
    public Map<String, Object> getProductWithAdminOverride(
            @PathVariable String productId,
            @RequestParam(defaultValue = "treatment_b") String cohort) throws Exception {

        var experimentCtx = new ExperimentContext(cohort, Map.of(
            "new-pricing-layout", true,
            "recommendations-v2", true
        ));

        return ScopedValue.where(ExperimentContext.CURRENT, experimentCtx)
                          .call(() -> featureFlagService.renderWithAdminOverride(productId));
    }
}
