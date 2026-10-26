package com.collective404.budget.service;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Purchase;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;
import com.collective404.budget.store.BudgetDataStore;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;
import com.collective404.budget.model.Budget;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.collective404.budget.model.Category;
import com.fasterxml.jackson.databind.JsonNode;
@Service
public class PurchaseService {

    private final BudgetDataStore dataStore;

    public PurchaseService(BudgetDataStore dataStore) {
        this.dataStore = dataStore;
    }
    private void validateAmount(BigDecimal amount) {
    if (amount == null
            || amount.compareTo(BigDecimal.ZERO) <= 0
            || amount.scale() > 2) {
        throw new IllegalArgumentException(
                "Amount must be positive and have at most 2 decimal places"
        );
    }
}
    public Purchase createPurchase(
        PurchaseRequest request, String categoryId
) throws IOException {
    validateAmount(request.getAmount());
    final String resolvedCategoryId =
        (categoryId == null || categoryId.isBlank()) ? "Misc" : categoryId;
    List<JsonNode> clients = dataStore.readList(
        "clients.json",
        new TypeReference<List<JsonNode>>() { }
);

boolean clientExists = clients.stream()
        .anyMatch(client ->
                request.getClientId() != null
                && request.getClientId().equals(
                        client.path("id").asText()
                )
        );

if (!clientExists) {
    throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Client not found"
    );
}

    List<Budget> budgets = dataStore.readList(
            "budgets.json",
            new TypeReference<List<Budget>>() { }
    );

    boolean budgetExists = budgets.stream()
            .anyMatch(budget ->
                    request.getBudgetId() != null
                    && request.getBudgetId().equals(budget.getId())
            );

    if (!budgetExists) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Budget not found"
        );
    }
List<Category> categories = dataStore.readList(
        "categories.json",
        new TypeReference<List<Category>>() { }
);

boolean categoryExists = categories.stream()
        .anyMatch(category ->
                request.getBudgetId() != null
                && request.getBudgetId().equals(category.getBudgetId())
                && resolvedCategoryId.equals(category.getId())
        );

if (!categoryExists) {
    throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Category not found"
    );
}
    return new Purchase(
            resolvedCategoryId,
            UUID.randomUUID().toString(),
            request.getName(),
            request.getAmount(),
            LocalDate.now().toString()
    );
}
public void savePurchase(Purchase purchase) throws IOException {
    dataStore.withExclusiveLock(() -> {
        List<Purchase> purchases = dataStore.readList(
                "purchases.json",
                new TypeReference<List<Purchase>>() { }
        );

        purchases.add(purchase);
        dataStore.writeList("purchases.json", purchases);

        return null;
    });
}
}
