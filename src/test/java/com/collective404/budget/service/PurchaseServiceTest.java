package com.collective404.budget.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

import com.collective404.budget.dto.PurchaseDeleteResponse;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
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

/**
 * Mockito-backed tests for {@link PurchaseService#deletePurchase}.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final String CLIENTS_FILE = "clients.json";
    private static final String BUDGETS_FILE = "budgets.json";
    private static final String CATEGORIES_FILE = "categories.json";
    private static final String PURCHASES_FILE = "purchases.json";

    private static final String CLIENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String BUDGET_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String OTHER_BUDGET_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    private static final String CATEGORY_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String PURCHASE_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";
    private static final String OTHER_PURCHASE_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    private static final String MISSING_ID = "00000000-0000-0000-0000-000000000000";
    private static final String DATE = "2026-10-01";

    @Mock
    private BudgetDataStore dataStore;

    private PurchaseService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new PurchaseService(dataStore);
        lenient().when(dataStore.withExclusiveLock(any())).thenAnswer(
                invocation -> invocation.<DataStoreOperation<?>>getArgument(0).execute());
    }

    private Category foodCategory() {
        return new Category(BUDGET_ID, CATEGORY_ID, "Food", new BigDecimal("100.00"));
    }

    private Purchase purchase(final String id, final String amount) {
        return new Purchase(CATEGORY_ID, id, "Snacks", new BigDecimal(amount), DATE);
    }

    private void stubClient() throws IOException {
        doReturn(List.of(CLIENT_ID)).when(dataStore).readList(eq(CLIENTS_FILE), any());
    }

    private void stubBudget() throws IOException {
        doReturn(List.of(new Budget(BUDGET_ID, "Main"))).when(dataStore).readList(eq(BUDGETS_FILE), any());
    }

    private void stubTwoPurchases() throws IOException {
        final List<Purchase> purchases = new ArrayList<>();
        purchases.add(purchase(PURCHASE_ID, "20.00"));
        purchases.add(purchase(OTHER_PURCHASE_ID, "30.00"));
        doReturn(purchases).when(dataStore).readList(eq(PURCHASES_FILE), any());
    }

    private void stubCategory() throws IOException {
        doReturn(List.of(foodCategory())).when(dataStore).readList(eq(CATEGORIES_FILE), any());
    }

    @Test
    void deleteReturnsRecalculatedSpent() throws IOException {
        stubClient();
        stubBudget();
        stubTwoPurchases();
        stubCategory();

        final PurchaseDeleteResponse result =
                service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        assertEquals(new BigDecimal("30.00"), result.getAmountSpent(),
                "spent should be the sum of the remaining purchases");
    }

    @Test
    void deleteReturnsRecalculatedRemaining() throws IOException {
        stubClient();
        stubBudget();
        stubTwoPurchases();
        stubCategory();

        final PurchaseDeleteResponse result =
                service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        assertEquals(new BigDecimal("70.00"), result.getAmountRemaining(),
                "remaining should be the limit minus the recalculated spent");
    }

    @Test
    void deleteReturnsDeletedPurchaseId() throws IOException {
        stubClient();
        stubBudget();
        stubTwoPurchases();
        stubCategory();

        final PurchaseDeleteResponse result =
                service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        assertEquals(PURCHASE_ID, result.getPurchaseId(),
                "response should echo the deleted purchase id");
    }

    @Test
    void deletePersistsRemovalOfThePurchase() throws IOException {
        stubClient();
        stubBudget();
        stubCategory();
        // Hold the same list the service mutates in place via removeIf.
        final List<Purchase> purchases = new ArrayList<>();
        purchases.add(purchase(PURCHASE_ID, "20.00"));
        purchases.add(purchase(OTHER_PURCHASE_ID, "30.00"));
        doReturn(purchases).when(dataStore).readList(eq(PURCHASES_FILE), any());

        service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID);

        final boolean stillPresent = purchases.stream()
                .anyMatch(p -> PURCHASE_ID.equals(p.getId()));
        assertFalse(stillPresent, "the deleted purchase should no longer be persisted");
    }

    @Test
    void deleteUnknownClientReturnsNull() throws IOException {
        doReturn(List.of()).when(dataStore).readList(eq(CLIENTS_FILE), any());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "deleting for an unknown client should return null");
    }

    @Test
    void deleteUnknownBudgetReturnsNull() throws IOException {
        stubClient();
        doReturn(List.of()).when(dataStore).readList(eq(BUDGETS_FILE), any());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "deleting for an unknown budget should return null");
    }

    @Test
    void deleteUnknownPurchaseReturnsNull() throws IOException {
        stubClient();
        stubBudget();
        stubTwoPurchases();

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, MISSING_ID),
                "deleting an unknown purchase should return null");
    }

    @Test
    void deletePurchaseInDifferentBudgetReturnsNull() throws IOException {
        stubClient();
        stubBudget();
        stubTwoPurchases();
        doReturn(List.of(new Category(OTHER_BUDGET_ID, CATEGORY_ID, "Food", new BigDecimal("100.00"))))
                .when(dataStore).readList(eq(CATEGORIES_FILE), any());

        assertNull(service.deletePurchase(CLIENT_ID, BUDGET_ID, PURCHASE_ID),
                "deleting a purchase whose category is in another budget should return null");
    }
}
