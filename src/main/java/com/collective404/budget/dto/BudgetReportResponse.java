package com.collective404.budget.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for GET /budget/{budgetId}.
 *
 * <p>Money values have 2 decimal places. Purchase amounts are rounded up,
 * remaining amounts are rounded down, and percent used is rounded half up.
 * Percent used is null when there is no positive limit to divide by.
 *
 * @param budgetId the budget ID
 * @param budgetName the budget name
 * @param totalLimit the sum of all category limits
 * @param totalSpent the sum of all purchases in the budget
 * @param totalRemaining total limit minus total spent (can be negative)
 * @param totalPercentUsed total spent as a percent of total limit
 * @param categories one row per category in the budget
 * @param purchases every purchase in the budget, oldest first
 */
public record BudgetReportResponse(
        String budgetId,
        String budgetName,
        BigDecimal totalLimit,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        BigDecimal totalPercentUsed,
        List<CategoryReport> categories,
        List<PurchaseSummary> purchases
) {

    /**
     * Spending numbers for one category.
     *
     * @param categoryId the category ID
     * @param name the category name
     * @param limit the spending limit
     * @param spent the total of the category's purchases
     * @param remaining limit minus spent (negative when overspent)
     * @param percentUsed spent as a percent of limit
     */
    public record CategoryReport(
            String categoryId,
            String name,
            BigDecimal limit,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal percentUsed
    ) {
    }

    /**
     * One purchase as shown in the report.
     *
     * @param purchaseId the purchase ID
     * @param categoryId the ID of the category it counts under
     * @param categoryName the name of that category
     * @param name the purchase name
     * @param amount the purchase amount
     * @param date the purchase date
     */
    public record PurchaseSummary(
            String purchaseId,
            String categoryId,
            String categoryName,
            String name,
            BigDecimal amount,
            String date
    ) {
    }
}
