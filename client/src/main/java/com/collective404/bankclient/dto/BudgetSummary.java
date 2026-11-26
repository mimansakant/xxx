package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record BudgetSummary(
        String budgetId,
        String budgetName,
        String accountId,
        String accountName,
        BigDecimal totalLimit,
        BigDecimal totalSpent,
        BigDecimal totalRemaining
) {
}
