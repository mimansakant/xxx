package com.collective404.budget.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for client access to budgets when creating purchases. */
final class PurchaseServiceTest {

    private static final String CATEGORY_ID = "cat-1";
    private static final String CLIENT_ID = "client-1";
    private static final String BUDGET_ID = "budget-1";
    private static final BigDecimal PURCHASE_AMOUNT = new BigDecimal("12.50");

    /**
     * A different client must not be able to add a purchase to this budget.
     *
     * @throws IOException when mocked I/O fails
     */
    @Test
    void rejectsOtherClientsBudget() throws IOException {
        final PurchaseService service = serviceWithBudgetOwner("other-client");
        assertThrows(ResponseStatusException.class,
                () -> service.createPurchase(request(), CATEGORY_ID),
                "A client must not add purchases to another client's budget");
    }

    /**
     * Unauthorized purchases must return the forbidden HTTP status.
     *
     * @throws IOException when mocked I/O fails
     */
    @Test
    void rejectsOtherClientsBudgetWithForbiddenStatus() throws IOException {
        final PurchaseService service = serviceWithBudgetOwner("other-client");
        assertEquals(HttpStatus.FORBIDDEN, statusWhenCreatingPurchase(service),
                "Unauthorized budget access must be forbidden");
    }

    /**
     * The budget owner can create a purchase in the selected category.
     *
     * @throws IOException when mocked I/O fails
     */
    @Test
    void ownerPurchaseHasExpectedCategory() throws IOException {
        final PurchaseService service = serviceWithBudgetOwner(CLIENT_ID);
        final Purchase purchase = service.createPurchase(request(), CATEGORY_ID);
        assertEquals(CATEGORY_ID, purchase.getCategoryId(),
                "The purchase must use the requested category");
    }

    /**
     * The budget owner can create a purchase for the requested amount.
     *
     * @throws IOException when mocked I/O fails
     */
    @Test
    void ownerPurchaseHasExpectedAmount() throws IOException {
        final PurchaseService service = serviceWithBudgetOwner(CLIENT_ID);
        final Purchase purchase = service.createPurchase(request(), CATEGORY_ID);
        assertEquals(PURCHASE_AMOUNT, purchase.getAmount(),
                "The purchase must keep the requested amount");
    }

    private HttpStatusCode statusWhenCreatingPurchase(
            final PurchaseService service) throws IOException {
        try {
            service.createPurchase(request(), CATEGORY_ID);
            return HttpStatus.CREATED;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode();
        }
    }

    private PurchaseService serviceWithBudgetOwner(
            final String owner) throws IOException {
        final BudgetDataStore store = org.mockito.Mockito.mock(
                BudgetDataStore.class);
        when(store.readList(eq("clients.json"), any())).thenReturn(
                List.of(Map.of("id", CLIENT_ID)));
        when(store.readList(eq("budgets.json"), any())).thenReturn(
                List.of(new Budget(BUDGET_ID, "Budget", owner)));
        when(store.readList(eq("categories.json"), any())).thenReturn(
                List.of(new Category(BUDGET_ID, CATEGORY_ID, "Food",
                        new BigDecimal("100.00"))));
        return new PurchaseService(store);
    }

    private PurchaseRequest request() {
        final PurchaseRequest request = new PurchaseRequest();
        request.setClientId(CLIENT_ID);
        request.setBudgetId(BUDGET_ID);
        request.setName("Lunch");
        request.setAmount(PURCHASE_AMOUNT);
        request.setCategory(CATEGORY_ID);
        return request;
    }
}
