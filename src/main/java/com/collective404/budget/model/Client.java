package com.collective404.budget.model;

/**
 * Represents a client of the budget API, as stored in clients.json.
 */
public final class Client {

    /** The client ID. */
    private String id;

    /** The client name. */
    private String name;

    /** The secret key the client sends in the X-API-Key header. */
    private String apiKey;

    /**
     * Empty constructor.
     */
    public Client() {
        // This constructor is intentionally left empty.
    }

    /**
     * Constructs a client.
     *
     * @param id the client ID
     * @param name the client name
     * @param apiKey the client's API key
     */
    public Client(final String id, final String name, final String apiKey) {
        this.id = id;
        this.name = name;
        this.apiKey = apiKey;
    }

    /**
     * Gets the client's ID.
     *
     * @return the ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the client's name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the client's API key.
     *
     * @return the API key
     */
    public String getApiKey() {
        return this.apiKey;
    }

    /**
     * Sets the client's ID.
     *
     * @param id the ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Sets the client's name.
     *
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the client's API key.
     *
     * @param apiKey the API key
     */
    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }
}
