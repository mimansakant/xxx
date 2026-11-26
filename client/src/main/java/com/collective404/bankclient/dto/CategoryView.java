package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record CategoryView(
        String categoryId,
        String name,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentUsed
) {
}
