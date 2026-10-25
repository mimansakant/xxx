

package com.collective404.budget.controller;

import com.collective404.budget.dto.CategoryUpdateRequest;
import com.collective404.budget.model.Category;
import com.collective404.budget.service.BudgetService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

/**
 * REST controller for budget operations.
 */
@RestController
@RequestMapping("/v1")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowedHeaders = {"X-API-Key", "Content-Type"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH}
)
public final class BudgetController {

    private final BudgetService budgetService;

    /**
     * Constructs the budget controller.
     *
     * @param budgetService the budget service
     */
    public BudgetController(final BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Updates an existing budget category.
     *
     * @param request the category update request
     * @return the updated category or error response
     * @throws IOException if a datastore operation fails
     */
    @PatchMapping("/budget/category")
    public ResponseEntity<?> updateCategory(
            @RequestBody final CategoryUpdateRequest request)
            throws IOException {

        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", 400);
            error.put("reason", "missing required value");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = budgetService.updateCategory(
            request.getClientId(), request.getBudgetId(),
            request.getCategoryId(), request.getName(), request.getLimit());

        if (Boolean.TRUE.equals(result.get("success"))) {
            Category category = (Category) result.get("returnValue");

            Map<String, Object> response = new HashMap<>();
            response.put("id", category.getId());
            response.put("name", category.getName());
            response.put("limit", category.getLimit());
            response.put("amountSpent", BigDecimal.ZERO.setScale(2));
            response.put("remainingAmount", category.getLimit());

            return ResponseEntity.ok(response);
        }

        int statusCode = (Integer) result.get("error");
        return ResponseEntity.status(statusCode).body(result);
    }
}
