package com.collective404.bankclient.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BudgetApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String clientId;

    public BudgetApiClient(
            final RestClient newRestClient,
            @Value("${bank.service.base-url}") final String newBaseUrl,
            @Value("${bank.service.api-key}") final String newApiKey,
            @Value("${bank.service.client-id}") final String newClientId
    ) {
        restClient = newRestClient;
        baseUrl = newBaseUrl.replaceAll("/+$", "");
        apiKey = newApiKey;
        clientId = newClientId;
    }

    public ServiceReport getReport(final String budgetId) {
        requireConfiguration();
        return restClient.get()
                .uri(baseUrl + "/budget/{budgetId}", budgetId)
                .header("X-API-Key", apiKey)
                .retrieve()
                .body(ServiceReport.class);
    }

    public ServiceCategory createCategory(
            final String budgetId,
            final String budgetName,
            final String name,
            final BigDecimal limit
    ) {
        requireConfiguration();
        final CreateCategoryBody body = new CreateCategoryBody(
                budgetId, budgetName, name, limit);
        return restClient.post()
                .uri(baseUrl + "/budget/category")
                .header("X-API-Key", apiKey)
                .body(body)
                .retrieve()
                .body(ServiceCategory.class);
    }

    public Object updateCategory(
            final String budgetId,
            final String categoryId,
            final String name,
            final BigDecimal limit
    ) {
        requireConfiguration();
        return restClient.patch()
                .uri(baseUrl + "/budget/category")
                .body(new UpdateCategoryBody(
                        clientId, budgetId, categoryId, name, limit))
                .retrieve()
                .body(Object.class);
    }

    public ServicePurchase createPurchase(
            final String budgetId,
            final String name,
            final BigDecimal amount,
            final String categoryName
    ) {
        requireConfiguration();
        return restClient.post()
                .uri(baseUrl + "/purchases")
                .body(new CreatePurchaseBody(
                        clientId, budgetId, name, amount, categoryName))
                .retrieve()
                .body(ServicePurchase.class);
    }

    public void deletePurchase(
            final String purchaseId, final String budgetId) {
        requireConfiguration();
        final URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/purchases/{id}")
                .queryParam("clientId", clientId)
                .queryParam("budgetId", budgetId)
                .buildAndExpand(purchaseId)
                .encode()
                .toUri();
        restClient.delete().uri(uri).retrieve().toBodilessEntity();
    }

    private void requireConfiguration() {
        if (apiKey.isBlank() || clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure BANK_SERVICE_API_KEY and BANK_SERVICE_CLIENT_ID");
        }
    }

    public record ServiceReport(
            String budgetId,
            String budgetName,
            BigDecimal totalLimit,
            BigDecimal totalSpent,
            BigDecimal totalRemaining,
            BigDecimal totalPercentUsed,
            List<ServiceCategoryReport> categories,
            List<ServicePurchaseSummary> purchases
    ) {
    }

    public record ServiceCategoryReport(
            String categoryId,
            String name,
            BigDecimal limit,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal percentUsed
    ) {
    }

    public record ServicePurchaseSummary(
            String purchaseId,
            String categoryId,
            String categoryName,
            String name,
            BigDecimal amount,
            String date
    ) {
    }

    public record ServiceCategory(
            String id,
            String budgetId,
            String name,
            String budgetName,
            BigDecimal limit,
            BigDecimal amountSpent,
            BigDecimal remainingAmount
    ) {
    }

    public record ServicePurchase(
            String id,
            String categoryId,
            String name,
            BigDecimal amount,
            String date
    ) {
    }

    private record CreateCategoryBody(
            String budgetId,
            String budgetName,
            String name,
            BigDecimal limit
    ) {
    }

    private record UpdateCategoryBody(
            String clientId,
            String budgetId,
            String categoryId,
            String name,
            BigDecimal limit
    ) {
    }

    private record CreatePurchaseBody(
            String clientId,
            String budgetId,
            String name,
            BigDecimal amount,
            String category
    ) {
    }
}
