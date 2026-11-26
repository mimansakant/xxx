package com.collective404.bankclient.controller;

import com.collective404.bankclient.auth.CurrentUser;
import com.collective404.bankclient.dto.BudgetCreateRequest;
import com.collective404.bankclient.dto.BudgetReportView;
import com.collective404.bankclient.dto.BudgetSummary;
import com.collective404.bankclient.dto.CategoryRequest;
import com.collective404.bankclient.dto.PurchaseCreateRequest;
import com.collective404.bankclient.dto.PurchaseView;
import com.collective404.bankclient.service.ClientBudgetService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BudgetController {

    private final CurrentUser currentUser;
    private final ClientBudgetService budgets;

    public BudgetController(
            final CurrentUser newCurrentUser,
            final ClientBudgetService newBudgets
    ) {
        currentUser = newCurrentUser;
        budgets = newBudgets;
    }

    @GetMapping("/budgets")
    public List<BudgetSummary> list(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization) {
        return budgets.list(currentUser.requireId(authorization));
    }

    @PostMapping("/budgets")
    public BudgetReportView create(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @RequestBody final BudgetCreateRequest request) {
        return budgets.create(currentUser.requireId(authorization), request);
    }

    @GetMapping("/budgets/{budgetId}/report")
    public BudgetReportView report(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String budgetId,
            @RequestParam(required = false) final String from,
            @RequestParam(required = false) final String to,
            @RequestParam(required = false) final String accountId,
            @RequestParam(required = false) final String categoryId) {
        return budgets.report(currentUser.requireId(authorization),
                budgetId, from, to, accountId, categoryId);
    }

    @PostMapping("/budgets/{budgetId}/categories")
    public BudgetReportView createCategory(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String budgetId,
            @RequestBody final CategoryRequest request) {
        return budgets.createCategory(
                currentUser.requireId(authorization), budgetId, request);
    }

    @PatchMapping("/budgets/{budgetId}/categories/{categoryId}")
    public BudgetReportView updateCategory(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String budgetId,
            @PathVariable final String categoryId,
            @RequestBody final CategoryRequest request) {
        return budgets.updateCategory(currentUser.requireId(authorization),
                budgetId, categoryId, request);
    }

    @PostMapping("/budgets/{budgetId}/purchases")
    public PurchaseView createPurchase(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String budgetId,
            @RequestBody final PurchaseCreateRequest request) {
        return budgets.createPurchase(
                currentUser.requireId(authorization), budgetId, request);
    }
}
