package com.collective404.bankclient.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(
            final ResponseStatusException exception) {
        final HttpStatus status = HttpStatus.valueOf(
                exception.getStatusCode().value());
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "reason", exception.getReason() == null
                        ? status.getReasonPhrase() : exception.getReason()));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleServiceError(
            final RestClientResponseException exception) {
        final HttpStatus status = HttpStatus.BAD_GATEWAY;
        final int serviceStatus = exception.getStatusCode().value();
        final String reason = serviceStatus == HttpStatus.UNAUTHORIZED.value()
                ? "Budget service rejected the API key. Verify "
                    + "BANK_SERVICE_BASE_URL points to this repository's "
                    + "service and BANK_SERVICE_API_KEY matches the selected "
                    + "client in src/main/resources/data/clients.json, then "
                    + "restart the client."
                : "Budget service request failed (HTTP "
                    + serviceStatus + ")";
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "reason", reason));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "status", HttpStatus.BAD_GATEWAY.value(),
                "reason", "Budget service could not be reached"));
    }
}
