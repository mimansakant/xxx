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
 * Mockito-backed tests for budget report calculations.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final String BUDGET_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_BUDGET_ID = "99999999-9999-9999-9999-999999999999";
    private static final String FOOD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String RENT_ID = "33333333-3333-3333-3333-333333333333";
    private static final String OTHER_CAT_ID = "44444444-4444-4444-4444-444444444444";
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Mock
    private BudgetDataStore dataStore;

    private BudgetService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new BudgetService(dataStore);
        lenient().when(dataStore.withExclusiveLock(any())).thenAnswer(
                invocation -> invocation.<DataStoreOperation<?>>getArgument(0).execute());
    }

    private static BigDecimal money(final String value) {
        return new BigDecimal(value);
    }

    private static Category category(final String id, final String name, final String limit) {
        return new Category(BUDGET_ID, id, name, money(limit));
    }

    private static Purchase purchase(
            final String id,
            final String categoryId,
            final String amount,
            final String date
    ) {
        return new Purchase(categoryId, id, "item " + id, money(amount), date);
    }

    private void givenData(
            final List<Category> categories,
            final List<Purchase> purchases
    ) throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(BUDGET_ID, "Fall semester", "clientId")));
        when(dataStore.<Category>readList(eq(BudgetService.CATEGORIES_FILE), any()))
                .thenReturn(categories);
        when(dataStore.<Purchase>readList(eq(BudgetService.PURCHASES_FILE), any()))
                .thenReturn(purchases);
    }

    @Test
    void unknownBudgetReturnsNullAndSkipsPurchases() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(OTHER_BUDGET_ID, "Someone else", "clientId")));

        assertNull(service.getBudgetReport(BUDGET_ID));
        verify(dataStore, never()).readList(eq(BudgetService.PURCHASES_FILE), any());
    }

    @Test
    void emptyBudgetsFileReturnsNull() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any())).thenReturn(null);

        assertNull(service.getBudgetReport(BUDGET_ID));
    }

    @Test
    void budgetWithNoCategoriesHasZeroTotals() throws IOException {
        givenData(List.of(), List.of());
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(BUDGET_ID, report.budgetId()),
                () -> assertEquals("Fall semester", report.budgetName()),
                () -> assertEquals(ZERO, report.totalSpent()),
                () -> assertEquals(ZERO, report.totalLimit()),
                () -> assertNull(report.totalPercentUsed()),
                () -> assertTrue(report.categories().isEmpty()),
                () -> assertTrue(report.purchases().isEmpty()));
    }

    @Test
    void categoryWithNoPurchasesShowsItsFullLimitRemaining() throws IOException {
        givenData(List.of(category(FOOD_ID, "Food", "200.00")), List.of());

        final CategoryReport food = service.getBudgetReport(BUDGET_ID).categories().get(0);

        assertAll(
                () -> assertEquals(money("0.00"), food.spent()),
                () -> assertEquals(money("200.00"), food.remaining()),
                () -> assertEquals(money("0.00"), food.percentUsed()));
    }

    @Test
    void computesCategoryAndOverallNumbers() throws IOException {
        givenData(
                List.of(category(FOOD_ID, "Food", "200.00"),
                        category(RENT_ID, "Rent", "1000.00")),
                List.of(purchase("p1", FOOD_ID, "75.50", "2026-10-02"),
                        purchase("p2", RENT_ID, "1000.00", "2026-10-01")));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);
        final CategoryReport food = report.categories().get(0);
        final CategoryReport rent = report.categories().get(1);

        assertAll(
                () -> assertEquals(money("75.50"), food.spent()),
                () -> assertEquals(money("124.50"), food.remaining()),
                () -> assertEquals(money("37.75"), food.percentUsed()),
                () -> assertEquals(ZERO, rent.remaining()),
                () -> assertEquals(money("100.00"), rent.percentUsed()),
                () -> assertEquals(money("1200.00"), report.totalLimit()),
                () -> assertEquals(money("1075.50"), report.totalSpent()),
                () -> assertEquals(money("124.50"), report.totalRemaining()),
                () -> assertEquals(money("89.63"), report.totalPercentUsed()));
    }

    @Test
    void overspentCategoryHasNegativeRemaining() throws IOException {
        givenData(List.of(category(FOOD_ID, "Food", "100.00")),
                List.of(purchase("p1", FOOD_ID, "150.00", "2026-10-01")));
        final CategoryReport food = service.getBudgetReport(BUDGET_ID).categories().get(0);

        assertAll(
                () -> assertEquals(money("-50.00"), food.remaining()),
                () -> assertEquals(money("150.00"), food.percentUsed()));
    }

    @Test
    void roundsPurchasesUpAndRemainingDown() throws IOException {
        givenData(List.of(category(FOOD_ID, "Food", "10.00")),
                List.of(purchase("p1", FOOD_ID, "3.331", "2026-10-01")));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(money("3.34"), report.categories().get(0).spent()),
                () -> assertEquals(money("6.66"), report.categories().get(0).remaining()),
                () -> assertEquals(money("3.34"), report.purchases().get(0).amount()));
    }

    @Test
    void ignoresOtherBudgetsAndOrphanPurchases() throws IOException {
        givenData(
                List.of(category(FOOD_ID, "Food", "100.00"),
                        new Category(OTHER_BUDGET_ID, OTHER_CAT_ID, "Food", money("500.00"))),
                List.of(purchase("mine", FOOD_ID, "10.00", "2026-10-01"),
                        purchase("theirs", OTHER_CAT_ID, "99.00", "2026-10-01"),
                        purchase("orphan", "missing-category", "5.00", "2026-10-01")));
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(1, report.categories().size()),
                () -> assertEquals(1, report.purchases().size()),
                () -> assertEquals("mine", report.purchases().get(0).purchaseId()),
                () -> assertEquals(money("10.00"), report.totalSpent()));
    }

    @Test
    void purchaseShowsItsCategoryName() throws IOException {
        givenData(List.of(category(FOOD_ID, "Food", "100.00")),
                List.of(purchase("p1", FOOD_ID, "10.00", "2026-10-01")));

        assertEquals("Food", service.getBudgetReport(BUDGET_ID).purchases().get(0).categoryName());
    }

    @Test
    void purchasesAreSortedOldestFirst() throws IOException {
        givenData(List.of(category(FOOD_ID, "Food", "100.00")),
                List.of(purchase("late", FOOD_ID, "1.00", "2026-10-20"),
                        purchase("early", FOOD_ID, "1.00", "2026-10-01")));

        final List<BudgetReportResponse.PurchaseSummary> purchases =
                service.getBudgetReport(BUDGET_ID).purchases();

        assertEquals(List.of("early", "late"),
                purchases.stream().map(BudgetReportResponse.PurchaseSummary::purchaseId).toList());
    }

    @Test
    void nullCategoryAndPurchaseListsAreTreatedAsEmpty() throws IOException {
        givenData(null, null);
        final BudgetReportResponse report = service.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertTrue(report.categories().isEmpty()),
                () -> assertTrue(report.purchases().isEmpty()));
    }

    @Test
    void readFailureIsPassedUp() throws IOException {
        when(dataStore.<Budget>readList(eq(BudgetService.BUDGETS_FILE), any()))
                .thenThrow(new IOException("disk gone"));

        assertThrows(IOException.class, () -> service.getBudgetReport(BUDGET_ID));
    }

    @Test
    void roundingHelpersTreatNullAsZeroAndZeroLimitHasNoPercent() {
        assertAll(
                () -> assertEquals(ZERO, BudgetService.roundUp(null)),
                () -> assertEquals(ZERO, BudgetService.roundDown(null)),
                () -> assertNull(BudgetService.percentUsed(ZERO, money("5.00"))));
    }
}
