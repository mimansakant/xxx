package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record PurchaseUpdateRequest(
        String name,
        BigDecimal amount,
        String date,
        String categoryId
) {
}
