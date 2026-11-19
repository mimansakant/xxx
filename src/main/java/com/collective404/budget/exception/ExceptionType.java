package com.collective404.budget.exception;

import org.springframework.http.HttpStatus;

/**
 * Represents the types of runtime exceptions thrown by this service.
 */

public enum ExceptionType {
    /** A conflict between a request and existing data. */
    CONFLICT_EX(HttpStatus.CONFLICT, "Conflict"),
    /** An I/O issue with the service's datastore. */
    DATASTORE_EX(HttpStatus.INTERNAL_SERVER_ERROR, "Datastore unavailable"),
    /** An item specified in the request was not found in the existing data. */
    NOTFOUND_EX(HttpStatus.NOT_FOUND, "Not found"),
    /** The header of the caller did not include a valid API key. */
    UNAUTHORIZED_EX(HttpStatus.UNAUTHORIZED, "Unauthorized caller"),
    /** The request accessed data not owned by the caller. */
    FORBIDDEN_EX(HttpStatus.FORBIDDEN, "Forbidden request");

    /**
     * The HTTP status code to be returned by the API for the specified type
     * of exception.
     */
    private final HttpStatus status;

    /**
     * The base message to be returned by the API for the specified type
     * of exception.
     */
    private final String baseMessage;

    /**
     * Constructs an exception type with an HTTP status code and base message.
     *
     * @param status the HTTP status code
     * @param baseMessage the base message
     */
    ExceptionType(final HttpStatus status, final String baseMessage) {
        this.status = status;
        this.baseMessage = baseMessage;
    }

    /**
     * Gets the base message of the exception type.
     *
     * @return the baseMessage
     */
    public String getBaseMessage() {
        return baseMessage;
    }

    /**
     * Gets the HTTP status code of the exception type.
     *
     * @return the status
     */
    public HttpStatus getStatus() {
        return status;
    }

}
