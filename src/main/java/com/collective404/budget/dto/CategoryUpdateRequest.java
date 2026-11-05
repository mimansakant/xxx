
package com.collective404.budget.dto;

import java.math.BigDecimal;

/**
 * Request body for updating a budget category.
 */
public final class CategoryUpdateRequest {

    /** The client requesting the update. */
    private String clientId;
    /** The budget containing the category. */
    private String budgetId;
    /** The category to update. */
    private String categoryId;
    /** The replacement category name, if supplied. */
    private String name;
    /** The replacement spending limit, if supplied. */
    private BigDecimal limit;

    /** Creates an empty request for JSON deserialization. */
    public CategoryUpdateRequest() {
    }

    /**
     * Creates a category update request.
     *
     * @param clientId the client requesting the update
     * @param budgetId the budget containing the category
     * @param categoryId the category to update
     * @param name the replacement category name
     * @param limit the replacement spending limit
     */
    public CategoryUpdateRequest(
            final String clientId,
            final String budgetId,
            final String categoryId,
            final String name,
            final BigDecimal limit
    ) {
        this.clientId = clientId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.name = name;
        this.limit = limit;
    }

    /** @return the client ID */
    public String getClientId() {
        return clientId;
    }

    /** @param clientId the client ID */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /** @return the budget ID */
    public String getBudgetId() {
        return budgetId;
    }

    /** @param budgetId the budget ID */
    public void setBudgetId(final String budgetId) {
        this.budgetId = budgetId;
    }

    /** @return the category ID */
    public String getCategoryId() {
        return categoryId;
    }

    /** @param categoryId the category ID */
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }

    /** @return the replacement category name */
    public String getName() {
        return name;
    }

    /** @param name the replacement category name */
    public void setName(final String name) {
        this.name = name;
    }

    /** @return the replacement spending limit */
    public BigDecimal getLimit() {
        return limit;
    }

    /** @param limit the replacement spending limit */
    public void setLimit(final BigDecimal limit) {
        this.limit = limit;
    }
}
