package com.collective404.budget.controller;

import com.collective404.budget.dto.BudgetReportResponse;
import com.collective404.budget.service.BudgetService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for budgets and their categories.
 */
@RestController
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Constructs the controller.
     *
     * @param budgetService the budget service
     */
    public BudgetController(final BudgetService budgetService) {
        this.budgetService = budgetService;
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
        if (!isValidUuid(budgetId)) {
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
}
