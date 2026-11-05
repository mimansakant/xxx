package com.collective404.budget.service;

import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Business operations for purchases. */
@Service
public final class PurchaseService {

    /** Datastore used for JSON persistence. */
    private final BudgetDataStore dataStore;

    /**
     * Creates the purchase service.
     *
     * @param dataStore datastore dependency
     */
    public PurchaseService(final BudgetDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Creates a purchase after checking client, budget and category existence.
     * Input validation is performed by the controller's InputValidator call.
     *
     * @param request validated purchase input
     * @param categoryId category requested by the caller
     * @return purchase to persist
     * @throws IOException if reading the datastore fails
     */
    public Purchase createPurchase(
            final PurchaseRequest request,
            final String categoryId) throws IOException {
        final List<Map<String, Object>> clients = dataStore.readList(
                "clients.json", new TypeReference<>() { });
        final boolean clientExists = clients.stream().anyMatch(client ->
                request.getClientId().trim().equals(client.get("id")));
        if (!clientExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Client not found");
        }

        final List<Budget> budgets = dataStore.readList(
                "budgets.json", new TypeReference<>() { });
        final Budget budget = budgets.stream()
                .filter(item -> request.getBudgetId().trim()
                        .equals(item.getId()))
                .findFirst().orElse(null);
        if (budget == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Budget not found");
        }
        if (!request.getClientId().trim().equals(budget.getClientId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Client cannot access this budget");
        }

        final String resolvedCategoryId =
                categoryId == null || categoryId.isBlank()
                        ? "Misc" : categoryId.trim();
        final List<Category> categories = dataStore.readList(
                "categories.json", new TypeReference<>() { });
        final Category matchingCategory = categories.stream()
                .filter(category ->
                        request.getBudgetId().trim()
                                .equals(category.getBudgetId())
                        && (resolvedCategoryId.equals(category.getId())
                            || resolvedCategoryId.equalsIgnoreCase(
                                    category.getName())))
                .findFirst().orElse(null);
        if (matchingCategory == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Category not found");
        }
        return new Purchase(
                matchingCategory.getId(),
                UUID.randomUUID().toString(),
                request.getName().trim(),
                request.getAmount(),
                LocalDate.now().toString());
    }

    /**
     * Saves a purchase to the datastore.
     *
     * @param purchase purchase to save
     * @throws IOException if accessing the datastore fails
     */
    public void savePurchase(final Purchase purchase) throws IOException {
        dataStore.withExclusiveLock(() -> {
            final List<Purchase> purchases = dataStore.readList(
                    "purchases.json", new TypeReference<>() { });
            purchases.add(purchase);
            dataStore.writeList("purchases.json", purchases);
            return null;
        });
    }
}
