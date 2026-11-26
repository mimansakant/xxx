package com.collective404.bankclient.dto;

public record AccountResponse(
        String id, String name, String type, String institution) {
}
