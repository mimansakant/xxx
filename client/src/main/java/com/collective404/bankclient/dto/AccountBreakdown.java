package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record AccountBreakdown(
        String accountId, String accountName, BigDecimal spent) {
}
