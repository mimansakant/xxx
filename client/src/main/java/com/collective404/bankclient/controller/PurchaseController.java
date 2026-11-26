package com.collective404.bankclient.controller;

import com.collective404.bankclient.auth.CurrentUser;
import com.collective404.bankclient.dto.PurchaseUpdateRequest;
import com.collective404.bankclient.dto.PurchaseView;
import com.collective404.bankclient.service.ClientBudgetService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final CurrentUser currentUser;
    private final ClientBudgetService budgets;

    public PurchaseController(
            final CurrentUser newCurrentUser,
            final ClientBudgetService newBudgets
    ) {
        currentUser = newCurrentUser;
        budgets = newBudgets;
    }

    @GetMapping
    public List<PurchaseView> list(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @RequestParam(required = false) final String from,
            @RequestParam(required = false) final String to,
            @RequestParam(required = false) final String accountId,
            @RequestParam(required = false) final String budgetId,
            @RequestParam(required = false) final String categoryId) {
        return budgets.listPurchases(currentUser.requireId(authorization),
                from, to, accountId, budgetId, categoryId);
    }

    @PatchMapping("/{purchaseId}")
    public PurchaseView update(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String purchaseId,
            @RequestBody final PurchaseUpdateRequest request) {
        return budgets.updatePurchase(
                currentUser.requireId(authorization), purchaseId, request);
    }

    @DeleteMapping("/{purchaseId}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String purchaseId) {
        budgets.deletePurchase(
                currentUser.requireId(authorization), purchaseId);
    }
}
