package com.collective404.budget.model;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Represents one category contained in a budget in the budget API.
 */

public final class Category {

    /** The ID of the budget to which the category belongs. */
    private String budgetId;

    /** The category ID. */
    private String id;

    /** The category name. */
    private String name;

    /** The category spending limit. */
    private BigDecimal limit;

    /**
     * Default empty constructor.
     */
    public Category() {
        // This constructor is intentionally left empty.
    }

    /**
     * Constructs a category.
     *
     * @param budgetId The budget ID to which the category belongs
     * @param id The category ID
     * @param name The category name
     * @param limit The category spending limit
     */
    public Category(String budgetId, final String id, String name, BigDecimal limit) {
        this.budgetId = budgetId;
        this.id = id;
        this.name = name;
        this.limit = limit;
    }

    /**
     * Gets the category's budget ID.
     *
     * @return the budget ID
     */
    public String getBudgetId() {
        return this.budgetId;
    }

    /**
     * Gets the category's ID.
     *
     * @return the ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the category's name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the category's spending limit.
     *
     * @return the spending limit
     */
    public BigDecimal getLimit() {
        return this.limit;
    }

    /**
     * Sets the category's budget ID.
     *
     * @param id The budget ID
     */
    public void setBudgetId(String id) {
        this.budgetId = budgetId;
    }

    /**
     * Sets the category's ID.
     *
     * @param id The ID
     */
    public void setId(final String id) {
       this.id = id;
    }

    /**
     * Sets the category's name.
     *
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the category's spending limit.
     *
     * @param limit The limit
     */
    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }

}