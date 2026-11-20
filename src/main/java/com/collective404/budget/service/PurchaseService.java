package com.collective404.budget.service;

import com.collective404.budget.dto.PurchaseDeleteResponse;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Client;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Business operations for purchases. */
@Service
public final class PurchaseService {

    /** Data file holding client records. */
    private static final String CLIENTS_FILE = "clients.json";

    /** Data file holding the list of budgets. */
    private static final String BUDGETS_FILE = "budgets.json";

    /** Data file holding the list of categories. */
    private static final String CATEGORIES_FILE = "categories.json";

    /** Data file holding the list of purchases. */
    private static final String PURCHASES_FILE = "purchases.json";

    /** Scale used for all monetary amounts. */
    private static final int MONEY_SCALE = 2;

    /** Store used to read and write budget data. */
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
        final List<Client> clients = dataStore.readList(
                CLIENTS_FILE, new TypeReference<>() { });
        final Client client = clients.stream()
                .filter(item -> request.getClientId().trim()
                        .equals(item.getId()))
                .findFirst().orElse(null);
        if (client == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Client not found");
        }

        final List<Budget> budgets = dataStore.readList(
                BUDGETS_FILE, new TypeReference<>() { });
        final Budget budget = budgets.stream()
                .filter(item -> request.getBudgetId().trim()
                        .equals(item.getId()))
                .findFirst().orElse(null);
        if (budget == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Budget not found");
        }
        if (!budgetBelongsToClient(budget, client)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Client cannot access this budget");
        }

        final String resolvedCategoryId =
                categoryId == null || categoryId.isBlank()
                        ? "Misc" : categoryId.trim();
        final List<Category> categories = dataStore.readList(
                CATEGORIES_FILE, new TypeReference<>() { });
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

    private boolean budgetBelongsToClient(
            final Budget budget,
            final Client client
    ) {
        final String budgetOwner = budget.getClientName();
        return budgetOwner != null
                && (budgetOwner.equalsIgnoreCase(client.getName())
                    || budgetOwner.equalsIgnoreCase(client.getId()));
    }

    /**
     * Deletes a purchase and returns the category's recalculated totals.
     *
     * <p>Returns {@code null} (which the controller maps to 404) when the
     * client does not exist, the budget does not exist or is not owned by the
     * client, the purchase does not exist, or the purchase's category does not
     * belong to the given budget. On success the purchase is removed and the
     * category's amount spent and amount remaining are recomputed from the
     * surviving purchases.</p>
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
            final String purchaseId) throws IOException {
        return dataStore.withExclusiveLock(() -> {
            final List<Client> clients = dataStore.readList(
                    CLIENTS_FILE, new TypeReference<>() { });
            final Client client = clients.stream()
                    .filter(item -> clientId.equals(item.getId()))
                    .findFirst().orElse(null);
            if (client == null) {
                return null;
            }

            final List<Budget> budgets = dataStore.readList(
                    BUDGETS_FILE, new TypeReference<>() { });
            final Budget budget = budgets.stream()
                    .filter(item -> budgetId.equals(item.getId()))
                    .findFirst().orElse(null);
            if (budget == null || !budgetBelongsToClient(budget, client)) {
                return null;
            }

            final List<Purchase> purchases = dataStore.readList(
                    PURCHASES_FILE, new TypeReference<>() { });
            final Purchase target = purchases.stream()
                    .filter(p -> purchaseId.equals(p.getId()))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                return null;
            }

            final List<Category> categories = dataStore.readList(
                    CATEGORIES_FILE, new TypeReference<>() { });
            final Category category = categories.stream()
                    .filter(c -> c.getId() != null
                            && c.getId().equals(target.getCategoryId())
                            && budgetId.equals(c.getBudgetId()))
                    .findFirst()
                    .orElse(null);
            if (category == null) {
                return null;
            }

            purchases.removeIf(p -> purchaseId.equals(p.getId()));
            dataStore.writeList(PURCHASES_FILE, purchases);

            final BigDecimal spent = purchases.stream()
                    .filter(p -> category.getId().equals(p.getCategoryId()))
                    .map(Purchase::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(MONEY_SCALE, RoundingMode.UP);
            final BigDecimal remaining = category.getLimit()
                    .subtract(spent)
                    .setScale(MONEY_SCALE, RoundingMode.DOWN);

            return new PurchaseDeleteResponse(
                    target.getId(), target.getAmount(), category.getId(),
                    spent, remaining);
        });
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
                    PURCHASES_FILE, new TypeReference<>() { });
            purchases.add(purchase);
            dataStore.writeList(PURCHASES_FILE, purchases);
            return null;
        });
    }
}
