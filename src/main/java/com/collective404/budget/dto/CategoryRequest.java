
package com.collective404.budget.dto;

import java.math.BigDecimal;

/**
 * Request body for creating a budget category.
 */
public final class CategoryRequest {

    private String clientId;
    private String budgetId;
    private String name;
    private BigDecimal limit;

    public CategoryRequest() {
    }

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(final String budgetId) {
        this.budgetId = budgetId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(final BigDecimal limit) {
        this.limit = limit;
    }
}
