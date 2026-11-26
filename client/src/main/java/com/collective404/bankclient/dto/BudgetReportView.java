package com.collective404.bankclient.dto;

import java.math.BigDecimal;
import java.util.List;

public record BudgetReportView(
        String budgetId,
        String budgetName,
        String accountId,
        String accountName,
        BigDecimal totalLimit,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        BigDecimal totalPercentUsed,
        List<CategoryView> categories,
        List<AccountBreakdown> accountBreakdown,
        List<PurchaseView> purchases
) {
}
