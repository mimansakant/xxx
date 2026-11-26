package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record PurchaseView(
        String purchaseId,
        String budgetId,
        String budgetName,
        String accountId,
        String accountName,
        String categoryId,
        String categoryName,
        String name,
        BigDecimal amount,
        String date,
        boolean categorySuggested
) {
}
