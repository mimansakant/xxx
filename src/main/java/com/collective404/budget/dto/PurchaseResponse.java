

package com.collective404.budget.dto;

import java.math.BigDecimal;

public class PurchaseResponse {
    private String id;
    private String categoryId;
    private String name;
    private BigDecimal amount;
    private String date;

    public PurchaseResponse() {
    }

    public PurchaseResponse(String id, String categoryId,
                            String name, BigDecimal amount,
                            String date) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.amount = amount;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
