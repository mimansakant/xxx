package com.collective404.budget.exception;

import org.springframework.http.HttpStatus;

/**
 * Represents an exception caused by a bad request:
 * a malformed input, missing parameters.
 */

public final class BadRequestException extends Exception {

    /** The default base message. */
    private static final String base = "Bad request";

    /** The detailed message. */
    private final String message;

    /**
     * Empty constructor constructs a BadRequestException with no additional
     * details.
     */
    public BadRequestException() {
        super();
        this.message = base;
    }

    /**
     * Constructs a BadRequestException with details regarding the cause of
     * the exception.
     *
     * @param detail the detailed message indicating the reason for the
     *               exception
     */
    public BadRequestException(final String detail) {
        super();
        this.message = base + ": " + detail;
    }

    /**
     * Gets the HTTP status code for a bad request.
     *
     * @return HTTP bad request status code
     */
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * Gets the exception's message.
     *
     * @return the message
     */
    @Override
    public String getMessage() {
        return message;
    }

}
