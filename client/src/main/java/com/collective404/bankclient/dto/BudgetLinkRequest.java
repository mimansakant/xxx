package com.collective404.bankclient.dto;

public record BudgetLinkRequest(
        String budgetId, String name, String accountId) {
}
