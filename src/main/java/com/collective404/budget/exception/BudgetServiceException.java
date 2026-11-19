package com.collective404.budget.exception;

import org.springframework.http.HttpStatus;

/**
 * Represents a runtime exception thrown by this budget service.
 */

public final class BudgetServiceException extends RuntimeException {

    /** The exception type of this exception. */
    private final ExceptionType type;

    /** The message for this exception. */
    private final String message;

    /**
     * Constructs a runtime exception of the given type with no detail message.
     *
     * @param type the type of exception
     */
    public BudgetServiceException(final ExceptionType type) {
        super();
        this.type = type;
        this.message = type.getBaseMessage();
    }

    /**
     * Constructs a runtime exception of the given type with a detail message.
     *
     * @param detail the detail message
     * @param type the type of exception
     */
    public BudgetServiceException(
            final String detail,
            final ExceptionType type
    ) {
        super();
        this.type = type;
        this.message = type.getBaseMessage() + ": " + detail;
    }

    /**
     * Gets the message of the exception.
     *
     * @return the message
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Gets the HTTP status code of the exception.
     *
     * @return the HTTP status code
     */
    public HttpStatus getStatus() {
        return this.type.getStatus();
    }

}
