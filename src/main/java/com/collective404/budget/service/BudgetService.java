package com.collective404.budget.service;

import com.collective404.budget.dto.BudgetReportResponse;
import com.collective404.budget.dto.BudgetReportResponse.CategoryReport;
import com.collective404.budget.dto.BudgetReportResponse.PurchaseSummary;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Client;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.collective404.budget.validation.InputValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Business logic for budgets and categories.
 */
@Service
public class BudgetService {

    /** Data file holding budgets. */
    static final String BUDGETS_FILE = "budgets.json";

    /** Data file holding categories. */
    static final String CATEGORIES_FILE = "categories.json";

    /** Data file holding purchases. */
    static final String PURCHASES_FILE = "purchases.json";

    /** Data file holding clients and their API keys. */
    static final String CLIENTS_FILE = "clients.json";

    /** A zero amount with two decimal places. */
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    /** The multiplier used to calculate percentages. */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Store used to read and write budget data. */
    private final BudgetDataStore dataStore;

    /**
     * Constructs the service.
     *
     * @param dataStore the shared data store
     */
    public BudgetService(final BudgetDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Creates a category in the caller's budget, creating a budget if needed.
     *
     * @param apiKey the API key from the X-API-Key header
     * @param budgetId the budget identifier, or {@code null} to create a budget
     * @param budgetName the name for a newly created budget
     * @param name the category name
     * @param limit the category spending limit
     * @return the created category or error message
     */
    public Map<String, Object> createCategory(
        final String apiKey,
        final String budgetId,
        final String budgetName,
        final String name,
        final BigDecimal limit
    ) throws IOException {
        return dataStore.withExclusiveLock(() -> createCategoryWithLock(
            apiKey,
            budgetId,
            budgetName,
            name,
            limit
        ));
    }

    private Map<String, Object> createCategoryWithLock(
        final String apiKey,
        final String budgetId,
        final String budgetName,
        final String name,
        final BigDecimal limit
    ) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<Client> allClients = dataStore.readList(
            CLIENTS_FILE,
            new TypeReference<>() { }
        );

        /* check 1: validate that the category values given are acc allowed */
        /* validate no nulls/empty strings */
        Client client = findClientByApiKey(allClients, apiKey);
        if (client == null) {
            result.put("success", false);
            result.put("error", 401);
            result.put("reason", "Invalid API key");
            return result;
        }
        if (name == null || limit == null) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "missing required value");
            return result;
        }
        List<Budget> allBudgets = dataStore.readList(
            BUDGETS_FILE,
            new TypeReference<>() { }
        );
        List<Category> allCategories = dataStore.readList(
            CATEGORIES_FILE,
            new TypeReference<>() { }
        );
        String newBudgetId = null;
        String resolvedBudgetName = null;
        if ((budgetId == null || budgetId.isBlank())
            && (budgetName == null || budgetName.isBlank())) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason",
                "budgetName is required when budgetId is omitted");
            return result;
        }
        /* validate limit outside of null*/
        if (!InputValidator.isValidLimit(limit.toString())) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "invalid limit");
            return result;
        }

        /*
         * Validate that the name is nonempty and contains at least one
         * character.
         */
        if (!name.matches(".*[A-Za-z].*")) {
            result.put("success", false);
            result.put("error", 400);
            result.put("reason", "invalid category name");
            return result;
        }

        /*
         * Check that a provided budget ID exists and belongs to the client.
         */
        if (budgetId != null && !budgetId.isBlank()) {
            Budget budget = allBudgets.stream()
                .filter(b -> b.getId().equals(budgetId.trim()))
                .findFirst()
                .orElse(null);
            if (budget == null) {
                result.put("success", false);
                result.put("error", 404);
                result.put("reason", "budgetId not found");
                return result;
            }
            if (budget.getClientId() == null
                || !budget.getClientId().equalsIgnoreCase(client.getId())) {
                result.put("success", false);
                result.put("error", 403);
                result.put("reason",
                    "budgetId does not belong to this client");
                return result;
            }

            /* check if category alr exists */
            Category existingCategory = allCategories.stream()
            .filter(category ->
                category.getBudgetId().equals(budgetId.trim())
                    &&
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

            /* Budget exists, the client exists, and the category is new. */
            /* so set new budgetId */
            newBudgetId = budgetId.trim();
            resolvedBudgetName = budget.getName();
        }

        /* if budgetId not given then create new budget */
        if (budgetId == null || budgetId.isBlank()) {
            Budget newBudget = new Budget(
                UUID.randomUUID().toString(),
                budgetName.trim(),
                    client.getId()
            );
            allBudgets.add(newBudget);
            dataStore.writeList(BUDGETS_FILE, allBudgets);
            newBudgetId = newBudget.getId();
            resolvedBudgetName = newBudget.getName();
        }

        /* atp budget exists so create new category */
        Category finalCategory = new Category(
            newBudgetId,
            UUID.randomUUID().toString(),
            name.trim(),
            limit.setScale(2, RoundingMode.DOWN)
        );
        allCategories.add(finalCategory);
        dataStore.writeList(CATEGORIES_FILE, allCategories);
        result.put("success", true);
        result.put("returnValue", finalCategory);
        result.put("budgetName", resolvedBudgetName);
        return result;
    }

    /**
     * Builds the spending report for one budget, if the caller owns it: spent,
     * remaining, and percent used per category, overall totals, and the
     * budget's purchases. Reads every file under one lock so the report is a
     * consistent snapshot. Does not modify any data.
     *
     * <p>Returns 401 if no client has the API key, 404 if no budget has the
     * ID, 403 if the budget belongs to a different client, and 200 with the
     * report otherwise.
     *
     * @param budgetId the budget to report on
     * @param apiKey the API key from the X-API-Key header
     * @return the report, or the status and reason it was refused
     * @throws IOException if the data files can't be read
     */
    public BudgetReportResult getBudgetReport(
            final String budgetId, final String apiKey) throws IOException {
        return dataStore.withExclusiveLock(
                () -> buildReportForClient(budgetId, apiKey));
    }

    private BudgetReportResult buildReportForClient(
            final String budgetId, final String apiKey) throws IOException {
        final Client client = findClientByApiKey(apiKey);
        if (client == null) {
            return BudgetReportResult.error(
                    BudgetReportResult.UNAUTHORIZED, "Invalid API key");
        }
        final Budget budget = findBudget(budgetId);
        if (budget == null) {
            return BudgetReportResult.error(
                    BudgetReportResult.NOT_FOUND,
                    "No budget found with id " + budgetId);
        }
        if (budget.getClientId() == null
                || !budget.getClientId().equalsIgnoreCase(client.getId())) {
            return BudgetReportResult.error(
                    BudgetReportResult.FORBIDDEN,
                    "budgetId does not belong to this client");
        }
        return BudgetReportResult.ok(buildReport(budget));
    }

    private BudgetReportResponse buildReport(final Budget budget)
            throws IOException {
        final String budgetId = budget.getId();

        final List<Category> categories = new ArrayList<>();
        for (final Category category : readCategories()) {
            if (budgetId.equals(category.getBudgetId())) {
                categories.add(category);
            }
        }

        final Map<String, Category> categoriesById = new HashMap<>();
        final Map<String, BigDecimal> spentById = new HashMap<>();
        for (final Category category : categories) {
            categoriesById.put(category.getId(), category);
            spentById.put(category.getId(), ZERO);
        }

        // Purchases don't store a budget ID, so a purchase belongs to this
        // budget
        // when its category does.
        BigDecimal totalSpent = ZERO;
        final List<PurchaseSummary> purchaseSummaries = new ArrayList<>();
        for (final Purchase purchase : readPurchases()) {
            final Category owner = categoriesById.get(purchase.getCategoryId());
            if (owner == null) {
                continue;
            }
            final BigDecimal amount = roundUp(purchase.getAmount());
            totalSpent = totalSpent.add(amount);
            spentById.merge(owner.getId(), amount, BigDecimal::add);
            purchaseSummaries.add(new PurchaseSummary(
                    purchase.getId(), owner.getId(), owner.getName(),
                    purchase.getName(), amount, purchase.getDate()));
        }

        BigDecimal totalLimit = ZERO;
        final List<CategoryReport> categoryReports = new ArrayList<>();
        for (final Category category : categories) {
            final BigDecimal limit = roundDown(category.getLimit());
            final BigDecimal spent = spentById.get(category.getId());
            totalLimit = totalLimit.add(limit);
            categoryReports.add(new CategoryReport(
                    category.getId(), category.getName(), limit, spent,
                    remaining(limit, spent), percentUsed(limit, spent)));
        }

        purchaseSummaries.sort(Comparator
                .comparing(
                        PurchaseSummary::date,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PurchaseSummary::purchaseId,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        return new BudgetReportResponse(
                budget.getId(), budget.getName(), totalLimit, totalSpent,
                remaining(totalLimit, totalSpent),
                percentUsed(totalLimit, totalSpent), categoryReports,
                purchaseSummaries);
    }

    private Client findClientByApiKey(final String apiKey)
            throws IOException {
        final List<Client> clients = dataStore.readList(
                CLIENTS_FILE, new TypeReference<List<Client>>() { });
        return findClientByApiKey(clients, apiKey);
    }

    private Client findClientByApiKey(
            final List<Client> clients,
            final String apiKey
    ) {
        if (apiKey != null && clients != null) {
            for (final Client client : clients) {
                if (apiKey.equals(client.getApiKey())) {
                    return client;
                }
            }
        }
        return null;
    }

    private Budget findBudget(final String budgetId) throws IOException {
        final List<Budget> budgets = dataStore.readList(
                BUDGETS_FILE, new TypeReference<List<Budget>>() { });
        if (budgets != null) {
            for (final Budget budget : budgets) {
                if (budgetId.equals(budget.getId())) {
                    return budget;
                }
            }
        }
        return null;
    }

    private List<Category> readCategories() throws IOException {
        final List<Category> categories = dataStore.readList(
                CATEGORIES_FILE, new TypeReference<List<Category>>() { });
        return categories == null ? List.of() : categories;
    }

    private List<Purchase> readPurchases() throws IOException {
        final List<Purchase> purchases = dataStore.readList(
                PURCHASES_FILE, new TypeReference<List<Purchase>>() { });
        return purchases == null ? List.of() : purchases;
    }

    /**
     * Rounds a purchase amount up to cents, as the team agreed for purchases.
     *
     * @param amount the amount, may be null
     * @return the rounded amount, or 0.00 if null
     */
    static BigDecimal roundUp(final BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.CEILING);
    }

    /**
     * Rounds a limit or remaining amount down to cents.
     *
     * @param amount the amount, may be null
     * @return the rounded amount, or 0.00 if null
     */
    static BigDecimal roundDown(final BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.FLOOR);
    }

    /**
     * Limit minus spent, rounded down. Negative when the limit is exceeded.
     *
     * @param limit the limit
     * @param spent the amount spent
     * @return the remaining amount
     */
    static BigDecimal remaining(
            final BigDecimal limit,
            final BigDecimal spent
    ) {
        return roundDown(limit.subtract(spent));
    }

    /**
     * Spent as a percent of limit, rounded half up to 2 decimals.
     *
     * @param limit the limit
     * @param spent the amount spent
     * @return the percent used, or null if the limit isn't positive
     */
    static BigDecimal percentUsed(
            final BigDecimal limit,
            final BigDecimal spent
    ) {
        if (limit.signum() <= 0) {
            return null;
        }
        return spent.multiply(HUNDRED).divide(limit, 2, RoundingMode.HALF_UP);
    }


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
    public Map<String, Object> updateCategory(
            final String clientId,
            final String budgetId,
            final String categoryId,
            final String name,
            final BigDecimal limit
    ) throws IOException {
        return dataStore.withExclusiveLock(() -> updateCategoryWithLock(
                clientId, budgetId, categoryId, name, limit));
    }

    private Map<String, Object> updateCategoryWithLock(
            final String clientId,
            final String budgetId,
            final String categoryId,
            final String name,
            final BigDecimal limit
    ) throws IOException {
        Map<String, Object> result = new HashMap<>();

        if (clientId == null || clientId.isBlank()
                || budgetId == null || budgetId.isBlank()
                || categoryId == null || categoryId.isBlank()) {
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

        List<String> allClients = dataStore.readList(
                "clients.json", new TypeReference<>() { });

        List<Budget> allBudgets = dataStore.readList(
                BUDGETS_FILE, new TypeReference<>() { });

        List<Category> allCategories = dataStore.readList(
                CATEGORIES_FILE, new TypeReference<>() { });


        boolean clientExists = allClients.stream()
                .anyMatch(c -> c.equalsIgnoreCase(clientId.trim()));

        if (!clientExists) {
            result.put("success", false);
            result.put("error", 404);
            result.put("reason", "clientId not found");
            return result;
        }

        Budget budget = allBudgets.stream()
                .filter(b -> b.getId().equals(budgetId.trim()))
                .findFirst()
                .orElse(null);

        if (budget == null || budget.getClientId() == null
                || !budget.getClientId().equalsIgnoreCase(clientId.trim())) {
            result.put("success", false);
            result.put("error", 404);
            result.put("reason", "budgetId not found");
            return result;
        }

        Category existingCategory = allCategories.stream()
            .filter(c -> c.getId().equals(categoryId.trim())
                    && c.getBudgetId().equals(budgetId.trim()))
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
                .filter(c -> c.getBudgetId().equals(budgetId.trim())
                    && !c.getId().equals(existingCategory.getId())
                    && c.getName().equalsIgnoreCase(name.trim()))
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
            PURCHASES_FILE, new TypeReference<>() { });

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

        dataStore.writeList(CATEGORIES_FILE, allCategories);
        result.put("success", true);
        result.put("returnValue", existingCategory);
        result.put("amountSpent", amountSpent);
        result.put("remainingAmount", remainingAmount);

        return result;
    }
}
