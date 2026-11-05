package com.collective404.budget.validation;

import java.math.BigDecimal;
import java.util.UUID;
import com.collective404.budget.dto.PurchaseRequest;

/**
 * Validator for API inputs.
 */

public final class InputValidator {

    /**
     * Checks required purchase fields and amount before calling the service.
     *
     * @param request purchase payload
     * @return true when the payload is valid
     */
    public static boolean isValidPurchaseRequest(
            final PurchaseRequest request) {
        return request != null
                && request.getClientId() != null
                && !request.getClientId().isBlank()
                && request.getBudgetId() != null
                && !request.getBudgetId().isBlank()
                && request.getName() != null
                && !request.getName().isBlank()
                && isValidAmount(request.getAmount());
    }

    private InputValidator() {
    }

    /**
     * Checks that an ID String is a UUID in standard form.
     *
     * @param id the ID to check
     * @return true if the ID is a valid UUID
     */
    public static boolean isValidUuid(final String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        try {
            return UUID.fromString(id).toString().equalsIgnoreCase(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks that a budget limit is greater than zero with no more than 2
     * decimal places.
     *
     * @param limit the limit to check
     * @return true if the limit is valid
     */
    public static boolean isValidLimit(final String limit) {
        BigDecimal limitNum;
        try {
            limitNum = new BigDecimal(limit);
        } catch (NumberFormatException e) {
            return false;
        }
        if (limitNum.scale() > 2 || limitNum.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks that a purchase amount is greater than zero with no more than 2
     * decimal places.
     *
     * @param amount the amount to check
     * @return true if the amount is valid
     */
    public static boolean isValidAmount(final BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2) {
            return false;
        }
        return true;
    }
}
