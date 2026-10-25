
package com.collective404.budget.dto;

import java.math.BigDecimal;

/**
 * Request body for updating a budget category.
 */
public final class CategoryUpdateRequest {

    private String clientId;
    private String budgetId;
    private String categoryId;
    private String name;
    private BigDecimal limit;

    public CategoryUpdateRequest() {
    }

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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
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