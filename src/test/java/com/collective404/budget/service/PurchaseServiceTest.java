package com.collective404.budget.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.collective404.budget.dto.PurchaseDeleteResponse;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Client;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.collective404.budget.store.BudgetDataStore.DataStoreOperation;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests for purchase creation, authorization, and deletion.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    /** Clients data file name. */
    private static final String CLIENTS_FILE = "clients.json";
    /** Budgets data file name. */
    private static final String BUDGETS_FILE = "budgets.json";
    /** Categories data file name. */
    private static final String CATEGORIES_FILE = "categories.json";
    /** Purchases data file name. */
    private static final String PURCHASES_FILE = "purchases.json";

    /** Id of the client under test. */
    private static final String CLIENT_ID =
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    /** Name of the client under test. */
    private static final String CLIENT_NAME = "Client One";
    /** Id of a different client. */
    private static final String OTHER_CLIENT_ID =
            "12121212-1212-1212-1212-121212121212";
    /** Id of the budget under test. */
    private static final String BUDGET_ID =
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    /** Id of a different budget. */
    private static final String OTHER_BUDGET_ID =
            "ffffffff-ffff-ffff-ffff-ffffffffffff";
    /** Id of the category under test. */
    private static final String CATEGORY_ID =
            "cccccccc-cccc-cccc-cccc-cccccccccccc";
    /** Id of the purchase to delete. */
    private static final String PURCHASE_ID =
            "dddddddd-dddd-dddd-dddd-dddddddddddd";
    /** Id of another purchase kept after deletion. */
    private static final String OTHER_PURCHASE_ID =
            "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    /** Id that matches no stored purchase. */
    private static final String MISSING_ID =
            "00000000-0000-0000-0000-000000000000";
    /** Fixed purchase date used in fixtures. */
    private static final String DATE = "2026-10-01";
    /** Amount used by purchase creation cases. */
    private static final BigDecimal PURCHASE_AMOUNT =
            new BigDecimal("12.50");

    /** Mock datastore used by the service. */
    @Mock
    private BudgetDataStore dataStore;

    /** Service under test. */
    private PurchaseService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new PurchaseService(dataStore);
        lenient().when(dataStore.withExclusiveLock(any())).thenAnswer(
                invocation -> invocation
                        .<DataStoreOperation<?>>getArgument(0).execute());
    }

    private Category foodCategory() {
        return new Category(
                BUDGET_ID, CATEGORY_ID, "Food", new BigDecimal("100.00"));
    }

    private Purchase purchase(final String id, final String amount) {
        return new Purchase(
                CATEGORY_ID, id, "Snacks", new BigDecimal(amount), DATE);
    }

    private List<Purchase> twoPurchases() {
        final List<Purchase> purchases = new ArrayList<>();
        purchases.add(purchase(PURCHASE_ID, "20.00"));
        purchases.add(purchase(OTHER_PURCHASE_ID, "30.00"));
        return purchases;
    }

    private void stubClient() throws IOException {
        doReturn(List.of(new Client(CLIENT_ID, CLIENT_NAME, "client-key")))
                .when(dataStore).readList(eq(CLIENTS_FILE), any());
    }

    private void stubBudgetOwnedBy(final String owner) throws IOException {
        doReturn(List.of(new Budget(BUDGET_ID, "Main", owner)))
                .when(dataStore).readList(eq(BUDGETS_FILE), any());
    }

    private void stubPurchases(final List<Purchase> purchases)
            throws IOException {
        doReturn(purchases)
                .when(dataStore).readList(eq(PURCHASES_FILE), any());
    }

    private void stubCategory() throws IOException {
        doReturn(List.of(foodCategory()))
                .when(dataStore).readList(eq(CATEGORIES_FILE), any());
    }

    @Test
    void deleteReturnsRecalculatedSpent() throws IOException {
        stubClient();
        stubBudgetOwnedBy(CLIENT_ID);
        stubPurchases(twoPurchases());
        stubCategory();

        final PurchaseDeleteResponse result =
                service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        assertEquals(new BigDecimal("30.00"), result.getAmountSpent(),
                "spent should be the sum of the remaining purchases");
    }

    @Test
    void deleteReturnsRecalculatedRemaining() throws IOException {
        stubClient();
        stubBudgetOwnedBy(CLIENT_ID);
        stubPurchases(twoPurchases());
        stubCategory();

        final PurchaseDeleteResponse result =
                service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        assertEquals(new BigDecimal("70.00"), result.getAmountRemaining(),
                "remaining should be the limit minus the recalculated spent");
    }

    @Test
    void deleteReturnsDeletedPurchaseId() throws IOException {
        stubClient();
        stubBudgetOwnedBy(CLIENT_ID);
        stubPurchases(twoPurchases());
        stubCategory();

        final PurchaseDeleteResponse result =
                service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        assertEquals(PURCHASE_ID, result.getPurchaseId(),
                "response should echo the deleted purchase id");
    }

    @Test
    void deletePersistsRemovalOfThePurchase() throws IOException {
        stubClient();
        stubBudgetOwnedBy(CLIENT_ID);
        stubCategory();
        final List<Purchase> purchases = twoPurchases();
        stubPurchases(purchases);

        service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        final boolean stillPresent = purchases.stream()
                .anyMatch(p -> PURCHASE_ID.equals(p.getId()));
        assertFalse(stillPresent,
                "the deleted purchase should no longer be persisted");
    }

    @Test
    void deleteUnknownClientReturnsNull() throws IOException {
        doReturn(List.of())
                .when(dataStore).readList(eq(CLIENTS_FILE), any());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "deleting for an unknown client should return null");
    }

    @Test
    void deleteBudgetOwnedByAnotherClientReturnsNull() throws IOException {
        stubClient();
        stubBudgetOwnedBy(OTHER_CLIENT_ID);

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "deleting from a budget owned by another client returns null");
    }

    @Test
    void deleteUnknownBudgetReturnsNull() throws IOException {
        stubClient();
        doReturn(List.of())
                .when(dataStore).readList(eq(BUDGETS_FILE), any());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "deleting for an unknown budget should return null");
    }

    @Test
    void deleteUnknownPurchaseReturnsNull() throws IOException {
        stubClient();
        stubBudgetOwnedBy(CLIENT_ID);
        stubPurchases(twoPurchases());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, MISSING_ID),
                "deleting an unknown purchase should return null");
    }

    @Test
    void deletePurchaseInDifferentBudgetReturnsNull() throws IOException {
        stubClient();
        stubBudgetOwnedBy(CLIENT_ID);
        stubPurchases(twoPurchases());
        doReturn(List.of(new Category(
                OTHER_BUDGET_ID, CATEGORY_ID, "Food",
                new BigDecimal("100.00"))))
                .when(dataStore).readList(eq(CATEGORIES_FILE), any());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "a purchase whose category is in another budget returns null");
    }

    @Test
    void rejectsOtherClientsBudget() throws IOException {
        final PurchaseService purchaseService =
                serviceWithBudgetOwner("other-client");
        assertThrows(ResponseStatusException.class,
                () -> purchaseService.createPurchase(
                        request(), CATEGORY_ID),
                "A client must not add purchases to another client's budget");
    }

    @Test
    void rejectsOtherClientsBudgetWithForbiddenStatus() throws IOException {
        final PurchaseService purchaseService =
                serviceWithBudgetOwner("other-client");
        assertEquals(HttpStatus.FORBIDDEN,
                statusWhenCreatingPurchase(purchaseService),
                "Unauthorized budget access must be forbidden");
    }

    @Test
    void ownerPurchaseHasExpectedCategory() throws IOException {
        final PurchaseService purchaseService =
                serviceWithBudgetOwner(CLIENT_NAME);
        final Purchase created = purchaseService.createPurchase(
                request(), CATEGORY_ID);
        assertEquals(CATEGORY_ID, created.getCategoryId(),
                "The purchase must use the requested category");
    }

    @Test
    void ownerPurchaseHasExpectedAmount() throws IOException {
        final PurchaseService purchaseService =
                serviceWithBudgetOwner(CLIENT_NAME);
        final Purchase created = purchaseService.createPurchase(
                request(), CATEGORY_ID);
        assertEquals(PURCHASE_AMOUNT, created.getAmount(),
                "The purchase must keep the requested amount");
    }

    private HttpStatusCode statusWhenCreatingPurchase(
            final PurchaseService purchaseService) throws IOException {
        try {
            purchaseService.createPurchase(request(), CATEGORY_ID);
            return HttpStatus.CREATED;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode();
        }
    }

    private PurchaseService serviceWithBudgetOwner(
            final String owner) throws IOException {
        final BudgetDataStore store = org.mockito.Mockito.mock(
                BudgetDataStore.class);
        when(store.readList(eq(CLIENTS_FILE), any())).thenReturn(
                List.of(new Client(CLIENT_ID, CLIENT_NAME, "client-key")));
        when(store.readList(eq(BUDGETS_FILE), any())).thenReturn(
                List.of(new Budget(BUDGET_ID, "Budget", owner)));
        when(store.readList(eq(CATEGORIES_FILE), any())).thenReturn(
                List.of(foodCategory()));
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
