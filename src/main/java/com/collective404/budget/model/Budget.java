package com.collective404.budget.model;
/**
 * Represents a budget in the budget API.
 */
public final class Budget {

    /** The budget ID. */
    private String id;

    /** The budget name. */
    private String name;

    /** The Name of the client that owns this budget. */
    private String clientName;

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
     * @param name the budget name
     * @param clientName the name of the owning client
     */
    public Budget(final String id, final String name, final String clientName) {
        this.id = id;
        this.name = name;
        this.clientName = clientName;
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
     * Gets the budget's client Name.
     *
     * @return the client name
     */
    public String getClientName() {
        return this.clientName;
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
     * @param name the budget name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the budget's client name.
     *
     * @param clientName the budget client name
     */
    public void setClientId(final String clientName) {
        this.clientName = clientName;
    }

}
