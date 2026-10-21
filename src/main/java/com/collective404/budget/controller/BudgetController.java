package com.collective404.budget.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.collective404.budget.dto.CategoryRequest;
import com.collective404.budget.dto.CategoryResponse;
import com.collective404.budget.model.Category;
import com.collective404.budget.service.BudgetService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for the BudgetService API.
 */
@RestController
@RequestMapping("/v1")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowedHeaders = {"X-API-Key", "Content-Type"},
    methods = {RequestMethod.GET, RequestMethod.POST}
)
public final class BudgetController {

        private final BudgetService budgetService;

        /**
         * The api controller constructor
         * @param budgetService the budgetAPIService
         */
        public BudgetController(
                final BudgetService budgetService
        ) {
                this.budgetService = budgetService;
        }


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

}
