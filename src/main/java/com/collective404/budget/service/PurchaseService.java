package com.collective404.budget.service;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.dto.PurchaseDeleteResponse;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    List<String> clients = dataStore.readList(
    "clients.json",
    new TypeReference<List<String>>() { }
);

boolean clientExists = clients.stream()
        .anyMatch(client ->
                request.getClientId() != null
                && client.equalsIgnoreCase(
                        request.getClientId().trim()
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
    }
}

    /**
     * Deletes a purchase and returns the affected category's recalculated totals.
     *
     * <p>Returns {@code null} (which the controller maps to 404) when the client
     * or budget does not exist, the purchase does not exist, or the purchase's
     * category does not belong to the given budget. On success the purchase is
     * removed from the datastore and the category's amount spent and amount
     * remaining are recomputed from the surviving purchases.</p>
     *
     * @param clientId the caller's client id
     * @param budgetId the budget the purchase belongs to
     * @param purchaseId the id of the purchase to delete
     * @return the recalculated totals, or {@code null} if nothing was deleted
     * @throws IOException if a datastore operation fails
     */
    public PurchaseDeleteResponse deletePurchase(
            final String clientId,
            final String budgetId,
            final String purchaseId
    ) throws IOException {
        return dataStore.withExclusiveLock(() -> {
            final List<String> clients = dataStore.readList(
                    CLIENTS_FILE, new TypeReference<List<String>>() { });
            final boolean clientExists = clients.stream()
                    .anyMatch(id -> id != null && id.equals(clientId));
            if (!clientExists) {
                return null;
            }

            final List<Budget> budgets = dataStore.readList(
                    BUDGETS_FILE, new TypeReference<List<Budget>>() { });
            final boolean budgetExists = budgets.stream()
                    .anyMatch(budget -> budgetId.equals(budget.getId()));
            if (!budgetExists) {
                return null;
            }

            final List<Purchase> purchases = dataStore.readList(
                    PURCHASES_FILE, new TypeReference<List<Purchase>>() { });
            final Purchase target = purchases.stream()
                    .filter(purchase -> purchaseId.equals(purchase.getId()))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                return null;
            }

            final List<Category> categories = dataStore.readList(
                    CATEGORIES_FILE, new TypeReference<List<Category>>() { });
            final Category category = categories.stream()
                    .filter(cat -> cat.getId() != null
                            && cat.getId().equals(target.getCategoryId())
                            && budgetId.equals(cat.getBudgetId()))
                    .findFirst()
                    .orElse(null);
            if (category == null) {
                // The purchase's category does not belong to this budget.
                return null;
            }

            purchases.removeIf(purchase -> purchaseId.equals(purchase.getId()));
            dataStore.writeList(PURCHASES_FILE, purchases);

            final BigDecimal spent = purchases.stream()
                    .filter(purchase -> category.getId().equals(purchase.getCategoryId()))
                    .map(Purchase::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(MONEY_SCALE, RoundingMode.UP);
            final BigDecimal remaining = category.getLimit()
                    .subtract(spent)
                    .setScale(MONEY_SCALE, RoundingMode.DOWN);

            return new PurchaseDeleteResponse(
                    target.getId(),
                    target.getAmount(),
                    category.getId(),
                    spent,
                    remaining);
        }
    }
}
