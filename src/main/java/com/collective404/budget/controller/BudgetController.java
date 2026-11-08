package com.collective404.budget.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.collective404.budget.dto.CategoryRequest;
import com.collective404.budget.dto.CategoryResponse;
import com.collective404.budget.model.Category;
import com.collective404.budget.service.BudgetReportResult;
import com.collective404.budget.service.BudgetService;
import com.collective404.budget.dto.CategoryUpdateRequest;
import com.collective404.budget.validation.InputValidator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    /** Header that carries the caller's API key. */
    static final String API_KEY_HEADER = "X-API-Key";

    /** the service. */
    private final BudgetService budgetService;

    /**
     * The api controller constructor.
     *
     * @param budgetService the budgetAPIService
     */
    public BudgetController(
        final BudgetService budgetService
    ) {
        this.budgetService = budgetService;
    }

    /**
     * Creates a category, optionally in an existing budget
     * owned by the client.
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
            categoryRequest.getBudgetName(),
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
                .body(CategoryResponse.from(
                    category, (String) result.get("budgetName")));
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
 * Returns the spending report for one budget. The caller identifies itself
 * with its API key in the X-API-Key header and can only see its own budgets.
 *
 * <p>200 with the report, 400 if the ID isn't a UUID, 401 if the API key is
 * missing or unknown, 403 if the budget belongs to another client, 404 if no
 * budget has that ID, and 500 if the data files can't be read.
 *
 * @param budgetId the budget ID
 * @param apiKey the caller's API key
 * @return the report or an error body
 */
@GetMapping("/budget/{budgetId}")
public ResponseEntity<?> getBudgetReport(
    @PathVariable("budgetId") final String budgetId,
    @RequestHeader(value = API_KEY_HEADER, required = false)
    final String apiKey
) {
    if (!InputValidator.isValidUuid(budgetId)) {
        return error(HttpStatus.BAD_REQUEST,
            "budgetId must be a valid UUID");
    }
    if (apiKey == null || apiKey.isBlank()) {
        return error(HttpStatus.UNAUTHORIZED,
            "Missing " + API_KEY_HEADER + " header");
    }
    try {
        final BudgetReportResult result =
            budgetService.getBudgetReport(budgetId, apiKey.trim());
        if (result.isOk()) {
            return ResponseEntity.ok(result.report());
        }
        return error(HttpStatus.valueOf(result.status()), result.reason());
    } catch (IOException e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
            "Budget data is unavailable");
    }
}

/**
 * Builds an error response in the agreed shape:
 * status number plus a reason string.
 *
 * @param status the HTTP status
 * @param reason why the request failed
 * @return the error response
 */
static ResponseEntity<Map<String, Object>> error(
    final HttpStatus status, final String reason) {
    return ResponseEntity.status(status)
    .body(Map.of("status", status.value(), "reason", reason));
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

    Map<String, Object> result;
    try {
        result = budgetService.updateCategory(
            request.getClientId(), request.getBudgetId(),
            request.getCategoryId(), request.getName(), request.getLimit());
    } catch (IOException e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Budget data is unavailable");

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
