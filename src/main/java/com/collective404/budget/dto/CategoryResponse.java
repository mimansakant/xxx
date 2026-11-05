
package com.collective404.budget.dto;

import com.collective404.budget.model.Category;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Response body for a budget category.
 */
public final class CategoryResponse {

    /** The category ID. */
    private String id;
    /** The category name. */
    private String name;
    /** The category spending limit. */
    private BigDecimal limit;
    /** The total spent in the category. */
    private BigDecimal amountSpent;
    /** The unspent amount remaining in the category. */
    private BigDecimal remainingAmount;

    /** Creates an empty response for JSON deserialization. */
    public CategoryResponse() {
    }

    /**
     * Creates a category response.
     *
     * @param id the category ID
     * @param name the category name
     * @param limit the category spending limit
     * @param amountSpent the total spent in the category
     * @param remainingAmount the unspent amount remaining
     */
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

    /** @return the category ID */
    public String getId() {
        return id;
    }

    /** @param id the category ID */
    public void setId(final String id) {
        this.id = id;
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

    /** @return the total spent in the category */
    public BigDecimal getAmountSpent() {
        return amountSpent;
    }

    /** @param amountSpent the total spent in the category */
    public void setAmountSpent(final BigDecimal amountSpent) {
        this.amountSpent = amountSpent;
    }

    /** @return the unspent amount remaining */
    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    /** @param remainingAmount the unspent amount remaining */
    public void setRemainingAmount(final BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
}
