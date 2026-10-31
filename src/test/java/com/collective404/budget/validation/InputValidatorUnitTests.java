package com.collective404.budget.validation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertAll;

class InputValidatorUnitTests {

    /** A UUID used to verify accepted identifiers. */
    private static final String BUDGET_ID =
            "11111111-1111-1111-1111-111111111111";

    /**
     * Check that isValidUuid returns true for Strings representing valid
     * UUIDs, and false for other Strings, including null and empty.
     */
    @Test
    void uuidCheckRejectsBadInput() {
        String testID = UUID.randomUUID().toString();
        assertAll(
                "isValidUuid should accept valid UUID strings "
                        + "and reject all others",
                () -> assertTrue(InputValidator.isValidUuid(BUDGET_ID)),
                () -> assertTrue(InputValidator.isValidUuid(testID)),
                () -> assertFalse(InputValidator.isValidUuid(null)),
                () -> assertFalse(InputValidator.isValidUuid("  ")),
                () -> assertFalse(InputValidator.isValidUuid("")),
                () -> assertFalse(InputValidator.isValidUuid("1-1-1-1-1")));
    }

    /**
     * Checks that isValidLimit returns false for Strings with more than 2
     * decimal places, that are numbers less than or equal to zero, and that
     * are not numerical values; and returns true for Strings that represent
     * numerical values with up to 2 decimal places greater than zero.
     */
    @Test
    void isValidLimitCorrectness() {
        assertAll(
                () -> assertTrue(InputValidator.isValidLimit("100")),
                () -> assertTrue(InputValidator.isValidLimit("1.25")),
                () -> assertFalse(InputValidator.isValidLimit("1,000")),
                () -> assertFalse(InputValidator.isValidLimit("10.250")),
                () -> assertFalse(InputValidator.isValidLimit("twenty")),
                () -> assertFalse(InputValidator.isValidLimit("-10")),
                () -> assertFalse(InputValidator.isValidLimit("0"))
        );
    }


}
