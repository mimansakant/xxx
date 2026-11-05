
package com.collective404.budget.dto;

import java.math.BigDecimal;

public class PurchaseRequest {

    /** The ID of the client that owns the purchase */
    private String clientId;

    /** The ID of the budget that owns the purchase */
    private String budgetId;

    /** The name of the purchase */
    private String name;

    /** The amount of the purchase */
    private BigDecimal amount;

    /** The name of the category that owns the purchase */
    private String category;

    /**
     * Gets the ID of the client that owns the purchase
     *
     * @return the client ID
     */
    public final String getClientId() {
        return clientId;
    }

    /**
     * Sets the ID of the client that owns the purchase
     *
     * @param clientId the client ID
     */
    public final void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the ID of the budget that owns the purchase
     *
     * @return the budget ID
     */
    public final String getBudgetId() {
        return budgetId;
    }

    /**
     * Sets the ID of the budget that owns the purchase
     *
     * @param budgetId the budget ID
     */
    public final void setBudgetId(final String budgetId) {
        this.budgetId = budgetId;
    }

    /**
     * Gets the purchase name
     *
     * @return the purchase name
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the purchase name
     *
     * @param name the purchase name
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the purchase amount
     *
     * @return the purchase amount
     */
    public final BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the purchase amount
     *
     * @param amount the purchase amount (in dollars up to two decimal points)
     */
    public final void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the name of the category that owns the purchase
     *
     * @return the category name
     */
    public final String getCategory() {
        return category;
    }

    /**
     * Sets the name of the category that owns the purchase
     *
     * @param category the category name
     */
    public final void setCategory(final String category) {
        this.category = category;
    }
}

