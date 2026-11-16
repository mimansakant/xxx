
package com.collective404.budget.dto;

import java.math.BigDecimal;

/**
 * Request body for creating a budget category.
 */
public final class CategoryRequest {

    /** The client creating the category. */
    private String clientId;
    /** The budget that will contain the category. */
    private String budgetId;
    /** The category name. */
    private String name;
    /** The category spending limit. */
    private BigDecimal limit;

    /** Creates an empty request for JSON deserialization. */
    public CategoryRequest() {
    }

    /**
     * Creates a category request.
     *
     * @param clientId the client creating the category
     * @param budgetId the budget that will contain the category
     * @param name the category name
     * @param limit the category spending limit
     */
    public CategoryRequest(
            final String clientId,
            final String budgetId,
            final String name,
            final BigDecimal limit
    ) {
        this.clientId = clientId;
        this.budgetId = budgetId;
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

    /** @return the category name */
    public String getName() {
        return name;
    }

    /** @param name the category name */
    public void setName(final String name) {
        this.name = name;
    }

    /** @return the category spending limit */
    public BigDecimal getLimit() {
        return limit;
    }

    /** @param limit the category spending limit */
    public void setLimit(final BigDecimal limit) {
        this.limit = limit;
    }
}
