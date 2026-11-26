package com.collective404.bankclient.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class ApiExceptionHandlerTest {

    @Test
    void unauthorizedServiceResponseExplainsHowToFixCredentials() {
        final ApiExceptionHandler handler = new ApiExceptionHandler();

        final var response = handler.handleServiceError(
                new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(String.valueOf(response.getBody().get("reason"))
                .contains("BANK_SERVICE_API_KEY"));
    }
}
