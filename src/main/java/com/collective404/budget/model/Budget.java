package com.collective404.budget.model;
import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Represents a budget in the budget API.
 */
public final class Budget {

    /** The budget ID. */
    private String id;

    /** The budget name. */
    private String name;

    /**
     * Empty constructor.
     */
    public Budget() {
        // This constructor is intentionally left empty.
    }

    /**
     * Constructs a budget.
     *
     * @param id The budget ID
     * @param name The budget name
     */
    public Budget(final String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the budget's ID.
     *
     * @return the ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the budget's name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the budget's ID.
     *
     * @param id the ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Sets the budget's name.
     *
     * @param name The budget name
     */
    public void setName(String name) {
        this.name = name;
    }

}
