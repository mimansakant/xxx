package com.collective404.budget.service;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Purchase;
import java.time.LocalDate;
import java.util.UUID;
import com.collective404.budget.store.BudgetDataStore;
import com.collective404.budget.validation.InputValidator;
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

    /** Datastore providing access to JSON files in resources/data directory. */
    private final BudgetDataStore dataStore;

    /**
     * Constructs a PurchaseService.
     *
     * @param dataStore the datastore with path to data directory
     */
    public PurchaseService(final BudgetDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Creates a new purchase from a request.
     *
     * @param request the PurchaseRequest with information for the new purchase
     * @param categoryId the ID of the category that owns the purchase
     *
     * @return the new Purchase item
     * @throws IOException when reading or writing from datastore fails
     */
    public final Purchase createPurchase(
        final PurchaseRequest request, final String categoryId
    ) throws IOException {
        InputValidator.isValidAmount(request.getAmount());
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

    /**
     * Saves a purchase to the datastore.
     *
     * @param purchase the purchase to be saved
     * @throws IOException if reading or writing to the datastore fails
     */
    public final void savePurchase(final Purchase purchase) throws IOException {
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
