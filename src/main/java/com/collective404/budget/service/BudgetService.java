
package com.collective404.budget.service;

import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Defines budget-related operations.
 */
@Service
public final class BudgetService {

    private final BudgetDataStore dataStore;

    public BudgetService(final BudgetDataStore dataStore) {
        this.dataStore = dataStore;
    }

    // // dummy create category implementation for compilation
    // public Map<String, Object> createCategory(String clientId, String budgetId,
    //         String name,
    //         BigDecimal limit) throws IOException {
    //     throw new UnsupportedOperationException(
    //         "POST category not yet merged"
    //     );
    // }

    /**
     * Updates an existing budget category.
     *
     * @param clientId the client identifier
     * @param budgetId the budget identifier
     * @param categoryId the category identifier
     * @param name the new name, or null if unchanged
     * @param limit the new limit, or null if unchanged
     * @return the updated category or an error
     * @throws IOException if a datastore operation fails
     */
    public Map<String, Object> updateCategory(String clientId, String budgetId, String categoryId, String name, BigDecimal limit) throws IOException {
        return dataStore.withExclusiveLock(() -> updateCategoryWithLock(clientId, budgetId, categoryId, name, limit));
    }

    private Map<String, Object> updateCategoryWithLock(String clientId, String budgetId, String categoryId, String name, BigDecimal limit) throws IOException {
        Map<String, Object> result = new HashMap<>();

        if (clientId == null || clientId.isBlank() || budgetId == null || budgetId.isBlank() || categoryId == null || categoryId.isBlank()) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "missing required value");
            return result;
        }

        if (name == null && limit == null) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "no update fields provided");
            return result;
        }

        if (name != null && !name.matches(".*[A-Za-z].*")) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "invalid category name");
            return result;
        }

        if (limit != null && limit.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "invalid limit");
            return result;
        }

        List<String> allClients = dataStore.readList("clients.json", new TypeReference<>() { });

        List<Budget> allBudgets = dataStore.readList("budgets.json", new TypeReference<>() { });

        List<Category> allCategories = dataStore.readList("categories.json", new TypeReference<>() { });


        boolean clientExists = allClients.stream().anyMatch(c -> c.equalsIgnoreCase(clientId.trim()));

        if (!clientExists) {
            result.put("success", false);
            result.put("error", 404);
            result.put("reason", "clientId not found");
            return result;
        }

        boolean budgetExists = allBudgets.stream().anyMatch(b -> b.getId().equals(budgetId.trim()));

        if (!budgetExists) {
            result.put("success", false);
            result.put("error", 404);
            result.put("reason", "budgetId not found");
            return result;
        }

        Category existingCategory = allCategories.stream()
            .filter(c -> c.getId().equals(categoryId.trim()) && c.getBudgetId().equals(budgetId.trim()))
            .findFirst()
            .orElse(null);

        if (existingCategory == null) {
            result.put("success", false);
            result.put("error", 404);
            result.put("reason", "categoryId not found");
            return result;
        }

        if (name != null) {
            Category duplicate = allCategories.stream()
                .filter(c -> c.getBudgetId().equals(budgetId.trim()) && !c.getId().equals(existingCategory.getId()) && c.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);

            if (duplicate != null) {
                result.put("success", false);
                result.put("error", 409);
                result.put("reason", "category with given name already exists");
                result.put("existingCategoryId", duplicate.getId());
                return result;
            }
        }

        // Calculate spending for the category
        List<Purchase> allPurchases = dataStore.readList(
            "purchases.json", new TypeReference<>() { });

        BigDecimal amountSpent = BigDecimal.ZERO.setScale(2);

        for (Purchase purchase : allPurchases) {
            if (purchase.getCategoryId().equals(existingCategory.getId())) {
                BigDecimal amount = purchase.getAmount()
                    .setScale(2, RoundingMode.CEILING);
                amountSpent = amountSpent.add(amount);
            }
        }

        if (name != null) {
            existingCategory.setName(name.trim());
        }

        if (limit != null) {
            existingCategory.setLimit(limit.setScale(2, RoundingMode.DOWN));
        }

        // calc remaining budget
        BigDecimal remainingAmount = existingCategory.getLimit()
            .subtract(amountSpent).setScale(2, RoundingMode.FLOOR);

        dataStore.writeList("categories.json", allCategories);
        result.put("success", true);
        result.put("returnValue", existingCategory);
        result.put("amountSpent", amountSpent);
        result.put("remainingAmount", remainingAmount);

        return result;
    }
}
