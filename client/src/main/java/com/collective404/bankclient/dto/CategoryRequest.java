package com.collective404.bankclient.dto;

import java.math.BigDecimal;

public record CategoryRequest(String name, BigDecimal limit) {
}
