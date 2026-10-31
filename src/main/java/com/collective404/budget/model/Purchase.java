package com.collective404.budget.model;

import java.math.BigDecimal;

public final class Purchase {

    /** The ID of the category that the purchase belongs to. */
    private String categoryId;

    /** The purchase ID. */
    private String id;

    /** The purchase name. */
    private String name;

    /** The purchase amount. */
    private BigDecimal amount;

    /** The purchase date. */
    private String date;

    /**
     * Constructs a purchase.
     *
     * @param categoryId the ID of the category that the purchase belongs to
     * @param id the purchase ID
     * @param name The purchase name
     * @param amount The purchase amount
     * @param date The purchase date
     */
    public Purchase(
            final String categoryId,
            final String id,
            final String name,
            final BigDecimal amount,
            final String date
    ) {
        this.categoryId = categoryId;
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.date = date;
    }

    /**
     * Gets the purchase's category ID.
     *
     * @return the category ID
     */
    public String getCategoryId() {
        return this.categoryId;
    }

    /**
     * Gets the purchase's ID.
     *
     * @return the ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the purchase's name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the purchase's amount.
     *
     * @return the amount
     */
    public BigDecimal getAmount() {
        return this.amount;
    }

    /**
     * Gets the purchase's date.
     *
     * @return the date
     */
    public String getDate() {
        return this.date;
    }

    /**
     * Sets the purchase's category ID.
     *
     * @param categoryId the category ID
     */
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * Sets the purchase's ID.
     *
     * @param id the purchase ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Sets the purchase's name.
     *
     * @param name The name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the purchase's amount.
     *
     * @param amount The amount
     */
    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Sets the purchase's date.
     *
     * @param date The date
     */
    public void setDate(final String date) {
        this.date = date;
    }

}
