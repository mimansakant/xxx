package com.collective404.budget.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.collective404.budget.dto.PurchaseRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Unit tests for purchase request validation. */
final class InputValidatorUnitTests {

    /** Checks that the validator rejects invalid amounts. */
    @Test
    void rejectsInvalidPurchaseAmount() {
        final PurchaseRequest request = validRequest();
        request.setAmount(new BigDecimal("0"));
        assertFalse(InputValidator.isValidPurchaseRequest(request));
        request.setAmount(new BigDecimal("1.001"));
        assertFalse(InputValidator.isValidPurchaseRequest(request));
    }

    /** Checks that required fields are enforced. */
    @Test
    void rejectsMissingRequiredFields() {
        final PurchaseRequest request = validRequest();
        request.setBudgetId(" ");
        assertFalse(InputValidator.isValidPurchaseRequest(request));
    }

    /** Checks that normal payloads are accepted. */
    @Test
    void acceptsValidPurchase() {
        assertTrue(InputValidator.isValidPurchaseRequest(validRequest()));
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
