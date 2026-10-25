package com.collective404.budget.service;

import com.collective404.budget.dto.BudgetReportResponse;
import com.collective404.budget.dto.BudgetReportResponse.CategoryReport;
import com.collective404.budget.dto.BudgetReportResponse.PurchaseSummary;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

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
     * Builds the spending report for one budget: spent, remaining, and percent used per
     * category, overall totals, and the budget's purchases. Reads all three files under one
     * lock so the report is a consistent snapshot. Does not modify any data.
     *
     * @param budgetId the budget to report on
     * @return the report, or null if no budget has this ID
     * @throws IOException if the data files can't be read
     */
    public BudgetReportResponse getBudgetReport(final String budgetId) throws IOException {
        return dataStore.withExclusiveLock(() -> buildReport(budgetId));
    }

    private BudgetReportResponse buildReport(final String budgetId) throws IOException {
        final Budget budget = findBudget(budgetId);
        if (budget == null) {
            return null;
        }

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

        // Purchases don't store a budget ID, so a purchase belongs to this budget
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
            purchaseSummaries.add(new PurchaseSummary(purchase.getId(), owner.getId(),
                    owner.getName(), purchase.getName(), amount, purchase.getDate()));
        }

        BigDecimal totalLimit = ZERO;
        final List<CategoryReport> categoryReports = new ArrayList<>();
        for (final Category category : categories) {
            final BigDecimal limit = roundDown(category.getLimit());
            final BigDecimal spent = spentById.get(category.getId());
            totalLimit = totalLimit.add(limit);
            categoryReports.add(new CategoryReport(category.getId(), category.getName(), limit,
                    spent, remaining(limit, spent), percentUsed(limit, spent)));
        }

        purchaseSummaries.sort(Comparator
                .comparing(PurchaseSummary::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PurchaseSummary::purchaseId,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        return new BudgetReportResponse(budget.getId(), budget.getName(), totalLimit, totalSpent,
                remaining(totalLimit, totalSpent), percentUsed(totalLimit, totalSpent),
                categoryReports, purchaseSummaries);
    }

    private Budget findBudget(final String budgetId) throws IOException {
        final List<Budget> budgets = dataStore.readList(BUDGETS_FILE, new TypeReference<List<Budget>>() { });
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
        final List<Category> categories =
                dataStore.readList(CATEGORIES_FILE, new TypeReference<List<Category>>() { });
        return categories == null ? List.of() : categories;
    }

    private List<Purchase> readPurchases() throws IOException {
        final List<Purchase> purchases =
                dataStore.readList(PURCHASES_FILE, new TypeReference<List<Purchase>>() { });
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
    static BigDecimal remaining(final BigDecimal limit, final BigDecimal spent) {
        return roundDown(limit.subtract(spent));
    }

    /**
     * Spent as a percent of limit, rounded half up to 2 decimals.
     *
     * @param limit the limit
     * @param spent the amount spent
     * @return the percent used, or null if the limit isn't positive
     */
    static BigDecimal percentUsed(final BigDecimal limit, final BigDecimal spent) {
        if (limit.signum() <= 0) {
            return null;
        }
        return spent.multiply(HUNDRED).divide(limit, 2, RoundingMode.HALF_UP);
    }
}
