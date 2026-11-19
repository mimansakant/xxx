package com.collective404.budget.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handler for the budget service.
 */
@ControllerAdvice
public final class GlobalExceptionHandler {

    /**
     * Exception handlers for BudgetServiceExceptions.
     *
     * @param e the exception
     * @return an HTTP response entity with the message and status
     * pertaining to the given exception
     */
    @ExceptionHandler(BudgetServiceException.class)
    public ResponseEntity<String> handleBudgetServiceException(
            final BudgetServiceException e
    ) {
        return new ResponseEntity<>(e.getMessage(), e.getStatus());
    }

    /**
     * Exception handlers for BadRequestExceptions.
     *
     * @param e the exception
     * @return an HTTP response entity with the message and status
     * pertaining to the given exception
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequestException(
            final BadRequestException e
    ) {
        return new ResponseEntity<>(e.getMessage(), e.getStatus());
    }

    /**
     * Exception handlers for other exceptions.
     *
     * @param e the exception
     * @return an HTTP response entity with the message of the
     * given exception and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(
            final Exception e
    ) {
        String body = makeExceptionBody(e);
        return new ResponseEntity<>(body,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates the body for the generic exception handler.
     *
     * @param e the given exception
     * @return a string with available details of the exception, if any
     */
    private static String makeExceptionBody(final Exception e) {
        String base = "An unexpected error occurred";
        String message = e.getMessage();
        Throwable cause = e.getCause();
        if (message != null && !message.isBlank()) {
            base += ": " + message;
        } else if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && !causeMessage.isBlank()) {
                base += ": " + causeMessage;
            } else {
                base += ": " + cause.getLocalizedMessage();
            }
        }
        return base;
    }
}
