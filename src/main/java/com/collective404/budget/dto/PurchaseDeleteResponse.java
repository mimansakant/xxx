package com.collective404.budget.dto;

import java.math.BigDecimal;

/**
 * Response body returned when a purchase is deleted.
 *
 * <p>Reports the deleted purchase's id, amount, and category, along with the
 * recalculated amount spent and amount remaining for that category after the
 * purchase has been removed.</p>
 */
public final class PurchaseDeleteResponse {

    /** The id of the purchase that was deleted. */
    private final String purchaseId;

    /** The amount of the purchase that was deleted. */
    private final BigDecimal amount;

    /** The id of the category the deleted purchase belonged to. */
    private final String categoryId;

    /** The category's amount spent after the deletion. */
    private final BigDecimal amountSpent;

    /** The category's amount remaining after the deletion. */
    private final BigDecimal amountRemaining;

    /**
     * Constructs a delete response.
     *
     * @param newPurchaseId the id of the deleted purchase
     * @param newAmount the amount of the deleted purchase
     * @param newCategoryId the category the deleted purchase belonged to
     * @param newAmountSpent the recalculated amount spent for the category
     * @param newAmountRemaining the recalculated amount remaining
     */
    public PurchaseDeleteResponse(
            final String newPurchaseId,
            final BigDecimal newAmount,
            final String newCategoryId,
            final BigDecimal newAmountSpent,
            final BigDecimal newAmountRemaining) {
        this.purchaseId = newPurchaseId;
        this.amount = newAmount;
        this.categoryId = newCategoryId;
        this.amountSpent = newAmountSpent;
        this.amountRemaining = newAmountRemaining;
    }

    /**
     * Gets the deleted purchase's id.
     *
     * @return the purchase id
     */
    public String getPurchaseId() {
        return purchaseId;
    }

    /**
     * Gets the deleted purchase's amount.
     *
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Gets the category id the deleted purchase belonged to.
     *
     * @return the category id
     */
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * Gets the category's recalculated amount spent.
     *
     * @return the amount spent
     */
    public BigDecimal getAmountSpent() {
        return amountSpent;
    }

    /**
     * Gets the category's recalculated amount remaining.
     *
     * @return the amount remaining
     */
    public BigDecimal getAmountRemaining() {
        return amountRemaining;
    }
}
