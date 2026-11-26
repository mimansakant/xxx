package com.collective404.bankclient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_metadata")
public class PurchaseMetadata {

    @Id
    @Column(length = 36)
    private String servicePurchaseId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 36)
    private String budgetId;

    @Column(length = 36)
    private String accountId;

    @Column(length = 36)
    private String categoryOverride;

    @Column(length = 200)
    private String nameOverride;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountOverride;

    @Column(length = 10)
    private String dateOverride;

    @Column(nullable = false)
    private boolean categorySuggested;

    protected PurchaseMetadata() {
    }

    public PurchaseMetadata(
            final String newServicePurchaseId,
            final Long newOwnerId,
            final String newBudgetId,
            final String newAccountId
    ) {
        servicePurchaseId = newServicePurchaseId;
        ownerId = newOwnerId;
        budgetId = newBudgetId;
        accountId = newAccountId;
    }

    public String getServicePurchaseId() {
        return servicePurchaseId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCategoryOverride() {
        return categoryOverride;
    }

    public String getNameOverride() {
        return nameOverride;
    }

    public BigDecimal getAmountOverride() {
        return amountOverride;
    }

    public String getDateOverride() {
        return dateOverride;
    }

    public boolean isCategorySuggested() {
        return categorySuggested;
    }

    public void setCategoryOverride(final String value) {
        categoryOverride = value;
    }

    public void setNameOverride(final String value) {
        nameOverride = value;
    }

    public void setAmountOverride(final BigDecimal value) {
        amountOverride = value;
    }

    public void setDateOverride(final String value) {
        dateOverride = value;
    }

    public void setCategorySuggested(final boolean value) {
        categorySuggested = value;
    }
}
