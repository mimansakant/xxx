package com.collective404.budget.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.collective404.budget.dto.BudgetReportResponse;
import com.collective404.budget.dto.CategoryRequest;
import com.collective404.budget.dto.CategoryResponse;
import com.collective404.budget.model.Category;
import com.collective404.budget.service.BudgetService;
import com.collective404.budget.dto.CategoryUpdateRequest;
import com.collective404.budget.validation.InputValidator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the BudgetService API.
 */
@RestController
@CrossOrigin(
    origins = "http://localhost:5500",
    allowedHeaders = {"X-API-Key", "Content-Type"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH}
)
public class BudgetController {

        private final BudgetService budgetService;

        /**
         * The api controller constructor
         *
         * @param budgetService the budgetAPIService
         */
        public BudgetController(
                final BudgetService budgetService
        ) {
                this.budgetService = budgetService;
        }

        /**
         * Creates a category, optionally in an existing budget owned by the client.
         *
         * @param categoryRequest the category and client details
         * @return the created category or an error response
         * @throws IOException if the data files can't be accessed
         */
        @PostMapping("/budget/category")
        public ResponseEntity<?> createCategory(
                @RequestBody final CategoryRequest categoryRequest
        ) throws IOException {
                if (categoryRequest == null) {
                        final Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("error", HttpStatus.BAD_REQUEST.value());
                        error.put("reason", "missing required value");
                        return ResponseEntity.badRequest().body(error);
                }

                final Map<String, Object> result = budgetService.createCategory(
                    categoryRequest.getClientId(),
                    categoryRequest.getBudgetId(),
                    categoryRequest.getName(),
                    categoryRequest.getLimit()
                );

                if (Boolean.TRUE.equals(result.get("success"))) {
                        final Object returnValue = result.get("returnValue");
                        if (!(returnValue instanceof Category category)) {
                                throw new IllegalStateException(
                                    "BudgetService returned an invalid category result"
                                );
                        }
                        return ResponseEntity.status(HttpStatus.CREATED)
                                .body(CategoryResponse.from(category));
                }

                final Object error = result.get("error");
                if (!(error instanceof Integer statusCode)) {
                        throw new IllegalStateException(
                            "BudgetService returned an invalid error status"
                        );
                }
                return ResponseEntity.status(statusCode).body(result);
        }

    /**
     * Returns the spending report for one budget.
     *
     * <p>200 with the report, 400 if the ID isn't a UUID, 404 if no budget has that ID,
     * and 500 if the data files can't be read.
     *
     * @param budgetId the budget ID
     * @return the report or an error body
     */
    @GetMapping("/budget/{budgetId}")
    public ResponseEntity<?> getBudgetReport(@PathVariable("budgetId") final String budgetId) {
        if (!InputValidator.isValidUuid(budgetId)) {
            return error(HttpStatus.BAD_REQUEST, "budgetId must be a valid UUID");
        }
        try {
            final BudgetReportResponse report = budgetService.getBudgetReport(budgetId);
            if (report == null) {
                return error(HttpStatus.NOT_FOUND, "No budget found with id " + budgetId);
            }
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Budget data is unavailable");
        }
    }

    /**
     * Builds an error response in the agreed shape: status number plus a reason string.
     *
     * @param status the HTTP status
     * @param reason why the request failed
     * @return the error response
     */
    static ResponseEntity<Map<String, Object>> error(final HttpStatus status, final String reason) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "reason", reason));
    }

    /**
     * Checks that an ID is a UUID in standard form. Move into InputValidator once it has rules.
     *
     * @param id the ID to check
     * @return true if the ID is a valid UUID
     */
    static boolean isValidUuid(final String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        try {
            return UUID.fromString(id).toString().equalsIgnoreCase(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Updates an existing budget category.
     *
     * @param request the category update request
     * @return the updated category or error response
     * @throws IOException if a datastore operation fails
     */
    @PatchMapping("/v1/budget/category")
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

        Map<String, Object> result;
        try {
            result = budgetService.updateCategory(
                request.getClientId(), request.getBudgetId(),
                request.getCategoryId(), request.getName(), request.getLimit());
        } catch (IOException e) {
                return error(HttpStatus.INTERNAL_SERVER_ERROR, "Budget data is unavailable"); 

        }

        if (Boolean.TRUE.equals(result.get("success"))) {
            Category category = (Category) result.get("returnValue");

            Map<String, Object> response = new HashMap<>();
            response.put("id", category.getId());
            response.put("name", category.getName());
            response.put("limit", category.getLimit());
            response.put("amountSpent", result.get("amountSpent"));
            response.put("remainingAmount", result.get("remainingAmount"));

            return ResponseEntity.ok(response);
        }

        int statusCode = (Integer) result.get("error");
        return ResponseEntity.status(statusCode).body(result);
    }
}
