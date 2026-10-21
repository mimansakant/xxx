
package com.collective404.budget.dto;

import com.collective404.budget.model.Category;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Response body for a budget category.
 */
public final class CategoryResponse {

    private String id;
    private String name;
    private BigDecimal limit;
    private BigDecimal amountSpent;
    private BigDecimal remainingAmount;

    public CategoryResponse() {
    }

    public CategoryResponse(
            final String id,
            final String name,
            final BigDecimal limit,
            final BigDecimal amountSpent,
            final BigDecimal remainingAmount
    ) {
        this.id = id;
        this.name = name;
        this.limit = limit;
        this.amountSpent = amountSpent;
        this.remainingAmount = remainingAmount;
    }

    /**
     * Creates the initial response for a newly created category.
     *
     * @param category the created category
     * @return the response with no spending recorded yet
     */
    public static CategoryResponse from(final Category category) {
        Objects.requireNonNull(category, "category must not be null");
        final BigDecimal initialAmountSpent = BigDecimal.ZERO.setScale(2);
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getLimit(),
            initialAmountSpent,
            category.getLimit().subtract(initialAmountSpent)
        );
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
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

    public BigDecimal getAmountSpent() {
        return amountSpent;
    }

    public void setAmountSpent(final BigDecimal amountSpent) {
        this.amountSpent = amountSpent;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(final BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
}
