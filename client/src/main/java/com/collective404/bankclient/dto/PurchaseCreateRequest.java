package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record PurchaseCreateRequest(
        String name,
        BigDecimal amount,
        String date,
        String categoryId
) {
}
