package com.collective404.budget.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.collective404.budget.dto.PurchaseRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Unit tests for purchase request validation. */
final class InputValidatorUnitTests {

    /** Checks that zero purchase amounts are rejected. */
    @Test
    void rejectsZeroPurchaseAmount() {
        final PurchaseRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);
        assertFalse(InputValidator.isValidPurchaseRequest(request),
                "A purchase amount must be positive");
    }

    /** Checks that amounts with too many decimal places are rejected. */
    @Test
    void rejectsPurchaseAmountWithThreeDecimals() {
        final PurchaseRequest request = validRequest();
        request.setAmount(new BigDecimal("1.001"));
        assertFalse(InputValidator.isValidPurchaseRequest(request),
                "A purchase amount must have no more than two decimals");
    }

    /** Checks that required fields are enforced. */
    @Test
    void rejectsMissingRequiredFields() {
        final PurchaseRequest request = validRequest();
        request.setBudgetId(" ");
        assertFalse(InputValidator.isValidPurchaseRequest(request),
                "A blank budget ID must be rejected");
    }

    /** Checks that normal payloads are accepted. */
    @Test
    void acceptsValidPurchase() {
        assertTrue(InputValidator.isValidPurchaseRequest(validRequest()),
                "A complete valid purchase request must be accepted");
    }

    private PurchaseRequest validRequest() {
        final PurchaseRequest request = new PurchaseRequest();
        request.setClientId("client-1");
        request.setBudgetId("budget-1");
        request.setName("Lunch");
        request.setAmount(new BigDecimal("12.50"));
        return request;
    }
}
