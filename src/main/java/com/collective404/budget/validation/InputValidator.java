package com.collective404.budget.validation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Validator for API inputs.
 */

public final class InputValidator {

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
     * Checks that a budget limit is greater than zero with 2 decimal places.
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
}
