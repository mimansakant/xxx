package com.collective404.budget.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.store.BudgetDataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines budget-related operations.
 */
@Service
public final class BudgetService {

    private final BudgetDataStore dataStore;

    public BudgetService(final BudgetDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Creates a category in the caller's budget, creating a budget if needed.
     *
     * @param clientId the caller's client identifier
     * @param budgetId the budget identifier, or {@code null} to create a budget
     * @param name the category name
     * @param limit the category spending limit
     * @return the created category or error message
     */
    public Map<String, Object> createCategory(
        String clientId,
        String budgetId,
        String name,
        BigDecimal limit) throws IOException {
        return dataStore.withExclusiveLock(() -> createCategoryWithLock(
            clientId,
            budgetId,
            name,
            limit
        ));
    }

    private Map<String, Object> createCategoryWithLock(
        String clientId,
        String budgetId,
        String name,
        BigDecimal limit) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<Budget> allBudgets = dataStore.readList(
            "budgets.json",
            new TypeReference<>() { }
        );
        List<String> allClients = dataStore.readList(
            "clients.json",
            new TypeReference<>() { }
        );
        List<Category> allCategories = dataStore.readList(
            "categories.json",
            new TypeReference<>() { }
        );
        String newBudgetId = null; /* id of budget new category will be added to */

        /* check 1: validate that the category values given are acc allowed */
        /* validate no nulls/empty strings */
        if (clientId == null || name == null || limit == null) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "missing required value");
            return result;
        }
        /* validate limit outside of null*/
        if (!(limit.compareTo(BigDecimal.ZERO) > 0)) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "invalid limit");
            return result;
        }

        /* validate name is nonempty and is a string involving at least 1 character */
        if (!name.matches(".*[A-Za-z].*")) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "invalid category name");
            return result;
        }

        /* validate clientID exists */
        /* note: i ASSUME clients.json just stores a list of clientIds */
        boolean clientExists = allClients.stream()
            .anyMatch(c ->
            c.equalsIgnoreCase(clientId.trim())
        );

        if (!clientExists) {
            result.put("success", false);
            result.put("error", 404);
            result.put("reason", "clientId not found");
            return result;
        }

        /* check 2: if budgetID given, check if exists. if not, tell to fuck off */
        if (budgetId != null && !budgetId.isEmpty()) {
            /* check if exists in data json */
            boolean budgetExists = allBudgets.stream()
            .anyMatch(b ->
                b.getId().equals(budgetId.trim())
            );
            if (!budgetExists) {
                result.put("success", false);
                result.put("error", 404);
                result.put("reason", "budgetId not found");
                return result;
            }

            /* check if category alr exists */
            Category existingCategory = allCategories.stream()
            .filter(category ->
                category.getBudgetId().equals(budgetId.trim()) &&
                category.getName().equalsIgnoreCase(name.trim())
            )
            .findFirst()
            .orElse(null);
            if (existingCategory != null) {
                result.put("success", false);
                result.put("error", 409);
                result.put("reason", "category with given name already exists");
                result.put("existingCategoryId", existingCategory.getId());
                return result;
            }

            /* getting here means budgetexists, client exists, and category dne */
            /* so set new budgetId */
            newBudgetId = budgetId.trim();
        }

        /* if budgetId not given then create new budget */
        if (budgetId == null || budgetId.isEmpty()) {
            Budget newBudget = new Budget(
                UUID.randomUUID().toString(),
                "dont_matter_yet"
            );
            allBudgets.add(newBudget);
            dataStore.writeList("budgets.json", allBudgets);
            newBudgetId = newBudget.getId();
        }

        /* atp budget exists so create new category */
        Category finalCategory = new Category(
            newBudgetId,
            UUID.randomUUID().toString(),
            name.trim(),
            limit.setScale(2, RoundingMode.DOWN)
        );
        allCategories.add(finalCategory);
        dataStore.writeList("categories.json", allCategories);
        result.put("success", true);
        result.put("returnValue", finalCategory);
        return result;
    }
}
