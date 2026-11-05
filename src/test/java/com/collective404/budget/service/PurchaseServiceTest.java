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
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for client access to budgets when creating purchases. */
final class PurchaseServiceTest {

    /**
     * A different client must not be able to add a purchase to this budget.
     *
     * @throws IOException when mocked I/O fails
     */
    @Test
    void rejectsOtherClientsBudget() throws IOException {
        final PurchaseService service = serviceWithBudgetOwner("other-client");
        final ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createPurchase(request(), "cat-1"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    /**
     * The client owning a budget is allowed to create its purchase.
     *
     * @throws IOException when mocked I/O fails
     */
    @Test
    void allowsBudgetOwner() throws IOException {
        final PurchaseService service = serviceWithBudgetOwner("client-1");
        final Purchase purchase = service.createPurchase(request(), "cat-1");
        assertEquals("cat-1", purchase.getCategoryId());
        assertEquals(new BigDecimal("12.50"), purchase.getAmount());
    }

    private PurchaseService serviceWithBudgetOwner(
            final String owner) throws IOException {
        final BudgetDataStore store = org.mockito.Mockito.mock(
                BudgetDataStore.class);
        when(store.readList(eq("clients.json"), any())).thenReturn(
                List.of(Map.of("id", "client-1")));
        when(store.readList(eq("budgets.json"), any())).thenReturn(
                List.of(new Budget("budget-1", "Budget", owner)));
        when(store.readList(eq("categories.json"), any())).thenReturn(
                List.of(new Category("budget-1", "cat-1", "Food",
                        new BigDecimal("100.00"))));
        return new PurchaseService(store);
    }

    private PurchaseRequest request() {
        final PurchaseRequest request = new PurchaseRequest();
        request.setClientId("client-1");
        request.setBudgetId("budget-1");
        request.setName("Lunch");
        request.setAmount(new BigDecimal("12.50"));
        request.setCategory("cat-1");
        return request;
    }
}
