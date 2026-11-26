package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record BudgetCreateRequest(
        String name,
        String accountId,
        String firstCategory,
        BigDecimal firstCategoryLimit
) {
}
