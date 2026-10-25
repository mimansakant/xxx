package com.collective404.budget.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collective404.budget.dto.BudgetReportResponse;
import com.collective404.budget.dto.BudgetReportResponse.CategoryReport;
import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.store.BudgetDataStore;
import com.collective404.budget.store.BudgetDataStore.DataStoreOperation;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the GET report logic in BudgetService. The data store is mocked.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final String BUDGET_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_BUDGET_ID = "99999999-9999-9999-9999-999999999999";
    private static final String FOOD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String RENT_ID = "33333333-3333-3333-3333-333333333333";
    private static final String OTHER_CAT_ID = "44444444-4444-4444-4444-444444444444";
    private static final String FOOD = "Food";
    private static final String DAY_ONE = "2026-10-01";
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String TEN = "10.00";
    private static final String LIMIT_100 = "100.00";
    private static final String LIMIT_1000 = "1000.00";
    private static final String LIMIT_200 = "200.00";

    @Mock
    private BudgetDataStore dataStore;

    private BudgetService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new BudgetService(dataStore);
        // Run the operation passed to withExclusiveLock, like the real store does.
        lenient().when(dataStore.withExclusiveLock(any())).thenAnswer(
                invocation -> invocation.<DataStoreOperation<?>>getArgument(0).execute());
    }

    private static BigDecimal money(final String value) {
        return new BigDecimal(value);
    }

    private static Category category(final String id, final String name, final String limit) {
        return new Category(BUDGET_ID, id, name, money(limit));
    }

    private static Purchase purchase(final String id, final String categoryId,
            final String amount, final String date) {
        return new Purchase(categoryId, id, "item " + id, money(amount), date);
    }

    private void givenData(final List<Category> categories, final List<Purchase> purchases)
            throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(BUDGET_ID, "Fall semester")));
        when(dataStore.<Category>readList(eq(BudgetService.CATEGORIES_FILE), any()))
                .thenReturn(categories);
        when(dataStore.<Purchase>readList(eq(BudgetService.PURCHASES_FILE), any()))
                .thenReturn(purchases);
    }

    @Test
    void unknownBudgetReturnsNull() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(OTHER_BUDGET_ID, "Someone else")));

        assertNull(service.getBudgetReport(BUDGET_ID), "unknown budget should give null");
    }

    @Test
    void unknownBudgetSkipsReadingPurchases() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(OTHER_BUDGET_ID, "Someone else")));

        service.getBudgetReport(BUDGET_ID);
        verify(dataStore, never()).readList(eq(BudgetService.PURCHASES_FILE), any());
    }

    @Test
    void emptyBudgetsFileReturnsNull() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any())).thenReturn(null);

        assertNull(service.getBudgetReport(BUDGET_ID), "empty budgets file should give null");
    }

    @Test
    void budgetWithNoCategoriesHasZeroTotals() throws IOException {
        givenData(List.of(), List.of());
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(BUDGET_ID, report.budgetId(), "budget id"),
                () -> assertEquals("Fall semester", report.budgetName(), "budget name"),
                () -> assertEquals(ZERO, report.totalSpent(), "empty total spent"),
                () -> assertEquals(ZERO, report.totalLimit(), "empty total limit"),
                () -> assertNull(report.totalPercentUsed(), "no limit means no percent"),
                () -> assertTrue(report.categories().isEmpty(), "no category rows"),
                () -> assertTrue(report.purchases().isEmpty(), "no purchase rows"));
    }

    @Test
    void categoryWithNoPurchasesShowsFullRemaining() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, LIMIT_200)), List.of());
        final CategoryReport food = service.getBudgetReport(BUDGET_ID).categories().get(0);

        assertAll(
                () -> assertEquals(ZERO, food.spent(), "nothing spent yet"),
                () -> assertEquals(money(LIMIT_200), food.remaining(), "full limit remains"),
                () -> assertEquals(ZERO, food.percentUsed(), "zero percent used"));
    }

    @Test
    void computesPerCategoryNumbers() throws IOException {
        givenData(
                List.of(category(FOOD_ID, FOOD, LIMIT_200), category(RENT_ID, "Rent", LIMIT_1000)),
                List.of(purchase("p1", FOOD_ID, "50.00", "2026-10-02"),
                        purchase("p2", FOOD_ID, "25.50", "2026-10-05"),
                        purchase("p3", RENT_ID, LIMIT_1000, DAY_ONE)));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);
        final CategoryReport food = report.categories().get(0);
        final CategoryReport rent = report.categories().get(1);

        assertAll(
                () -> assertEquals(money("75.50"), food.spent(), "food spent"),
                () -> assertEquals(money("124.50"), food.remaining(), "food remaining"),
                () -> assertEquals(money("37.75"), food.percentUsed(), "food percent used"),
                () -> assertEquals(ZERO, rent.remaining(), "rent remaining"),
                () -> assertEquals(money(LIMIT_100), rent.percentUsed(), "rent percent used"));
    }

    @Test
    void computesOverallTotals() throws IOException {
        givenData(
                List.of(category(FOOD_ID, FOOD, LIMIT_200), category(RENT_ID, "Rent", LIMIT_1000)),
                List.of(purchase("p1", FOOD_ID, "75.50", "2026-10-02"),
                        purchase("p3", RENT_ID, LIMIT_1000, DAY_ONE)));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(money("1200.00"), report.totalLimit(), "total limit"),
                () -> assertEquals(money("1075.50"), report.totalSpent(), "total spent"),
                () -> assertEquals(money("124.50"), report.totalRemaining(), "total remaining"),
                () -> assertEquals(money("89.63"), report.totalPercentUsed(), "total percent used"));
    }

    @Test
    void overspentCategoryHasNegativeRemaining() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, LIMIT_100)),
                List.of(purchase("p1", FOOD_ID, "150.00", DAY_ONE)));
        final CategoryReport food = service.getBudgetReport(BUDGET_ID).categories().get(0);

        assertAll(
                () -> assertEquals(money("-50.00"), food.remaining(), "overspent remaining is negative"),
                () -> assertEquals(money("150.00"), food.percentUsed(), "overspent percent above 100"));
    }

    @Test
    void purchasesRoundUpAndRemainingRoundsDown() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, TEN)),
                List.of(purchase("p1", FOOD_ID, "3.331", DAY_ONE)));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(money("3.34"), report.categories().get(0).spent(), "spent rounds up"),
                () -> assertEquals(money("6.66"), report.categories().get(0).remaining(), "remaining rounds down"),
                () -> assertEquals(money("3.34"), report.purchases().get(0).amount(), "purchase amount rounds up"));
    }

    @Test
    void ignoresCategoriesAndPurchasesFromOtherBudgets() throws IOException {
        givenData(
                List.of(category(FOOD_ID, FOOD, LIMIT_100),
                        new Category(OTHER_BUDGET_ID, OTHER_CAT_ID, FOOD, money("500.00"))),
                List.of(purchase("mine", FOOD_ID, TEN, DAY_ONE),
                        purchase("theirs", OTHER_CAT_ID, "99.00", DAY_ONE),
                        purchase("orphan", "no-such-category", "5.00", DAY_ONE)));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(1, report.categories().size(), "only this budget's categories"),
                () -> assertEquals(1, report.purchases().size(), "only this budget's purchases"),
                () -> assertEquals("mine", report.purchases().get(0).purchaseId(), "kept purchase"),
                () -> assertEquals(money(TEN), report.totalSpent(), "total counts only this budget"));
    }

    @Test
    void purchaseShowsItsCategoryName() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, LIMIT_100)),
                List.of(purchase("p1", FOOD_ID, TEN, DAY_ONE)));

        assertEquals(FOOD, service.getBudgetReport(BUDGET_ID).purchases().get(0).categoryName(),
                "purchase shows category name");
    }

    @Test
    void purchasesAreSortedOldestFirst() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, LIMIT_100)),
                List.of(purchase("late", FOOD_ID, "1.00", "2026-10-20"),
                        purchase("early", FOOD_ID, "1.00", DAY_ONE)));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals("early", report.purchases().get(0).purchaseId(), "oldest first"),
                () -> assertEquals("late", report.purchases().get(1).purchaseId(), "newest last"));
    }

    @Test
    void nullCategoryAndPurchaseListsAreTreatedAsEmpty() throws IOException {
        givenData(null, null);
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertTrue(report.categories().isEmpty(), "null categories become empty"),
                () -> assertTrue(report.purchases().isEmpty(), "null purchases become empty"));
    }

    @Test
    void readFailureIsPassedUp() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenThrow(new IOException("disk gone"));

        assertThrows(IOException.class, () -> service.getBudgetReport(BUDGET_ID),
                "read errors reach the controller");
    }

    @Test
    void percentUsedIsNullForZeroLimit() {
        assertNull(BudgetService.percentUsed(ZERO, money("5.00")), "zero limit has no percent");
    }

    @Test
    void roundingHelpersTreatNullAsZero() {
        assertAll(
                () -> assertEquals(ZERO, BudgetService.roundUp(null), "null rounds up to zero"),
                () -> assertEquals(ZERO, BudgetService.roundDown(null), "null rounds down to zero"));
    }
}
