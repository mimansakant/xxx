package com.collective404.budget.model;
/**
 * Represents a budget in the budget API.
 */
public final class Budget {

    /** The budget ID. */
    private String id;

    /** The budget name. */
    private String name;

    /** The ID of the client that owns this budget. */
    private String clientId;

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
     * @param clientId the ID of the owning client
     */
    public Budget(final String id, final String name, final String clientId) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
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
     * Gets the budget's client ID.
     *
     * @return the client ID
     */
    public String getClientId() {
        return this.clientId;
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
     * Sets the budget's client ID.
     *
     * @param clientId the budget client ID
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

}
