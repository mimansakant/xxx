

package com.collective404.budget.dto;

import java.math.BigDecimal;

public class PurchaseResponse {

    /** Purchase ID */
    private String id;

    /** ID of category that purchase is in */
    private String categoryId;

    /** Purchase name */
    private String name;

    /** Purchase amount */
    private BigDecimal amount;

    /** Purchase date */
    private String date;

    /** Default empty constructor */
    public PurchaseResponse() {
        // This constructor is intentionally left empty.
    }

    /** Constructs a PurchaseResponse
     *
     * @param id the purchase ID
     * @param categoryId the ID of the category the purchase is in
     * @param name the purchase name
     * @param amount the purchase amount
     * @param date the purchase date
     */
    public PurchaseResponse(final String id, final String categoryId,
                            final String name, final BigDecimal amount,
                            final String date) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.amount = amount;
        this.date = date;
    }

    /**
     * Gets the ID of the purchase
     *
     * @return the purchase ID
     */
    public final String getId() {
        return id;
    }

    /**
     * Sets the ID of the purchase
     *
     * @param id the purchase ID
     */
    public final void setId(final String id) {
        this.id = id;
    }

    /**
     * Gets the ID of the category the purchase is in
     *
     * @return the category ID
     */
    public final String getCategoryId() {
        return categoryId;
    }

    /**
     * Sets the ID of the category the purchase is in
     *
     * @param categoryId the ID of the category to put this purchase in
     */
    public final void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
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
     * @param name the name to assign to the purchase
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
     * @param amount the amount (in dollars up to two decimal points) of the purchase
     */
    public final void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the date of the purchase
     *
     * @return the purchase date
     */
    public final String getDate() {
        return date;
    }

    /**
     * Sets the date of the purchase
     *
     * @param date the purchase date (in DD-MM-YYYY format)
     */
    public final void setDate(final String date) {
        this.date = date;
    }
}
