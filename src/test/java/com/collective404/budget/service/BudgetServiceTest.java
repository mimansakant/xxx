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
import com.collective404.budget.dto.BudgetReportResponse.PurchaseSummary;
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
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mockito-backed tests for budget report calculations and access checks.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    /** Identifier of the budget under test. */
    private static final String BUDGET_ID =
            "11111111-1111-1111-1111-111111111111";
    /** Identifier of a different budget. */
    private static final String OTHER_BUDGET_ID =
            "99999999-9999-9999-9999-999999999999";
    /** Identifier of the food category. */
    private static final String FOOD_ID =
            "22222222-2222-2222-2222-222222222222";
    /** Identifier of the rent category. */
    private static final String RENT_ID =
            "33333333-3333-3333-3333-333333333333";
    /** Identifier of another budget's category. */
    private static final String OTHER_CAT_ID =
            "44444444-4444-4444-4444-444444444444";
    /** Identifier of the client that owns the budget. */
    private static final String CLIENT_ID =
            "55555555-5555-5555-5555-555555555555";
    /** API key of the client that owns the budget. */
    private static final String API_KEY =
            "66666666-6666-6666-6666-666666666666";
    /** Identifier of a client that doesn't own the budget. */
    private static final String OTHER_CLIENT_ID =
            "77777777-7777-7777-7777-777777777777";
    /** API key of the client that doesn't own the budget. */
    private static final String OTHER_API_KEY =
            "88888888-8888-8888-8888-888888888888";
    /** Client ID used by the createCategory tests. */
    private static final String PLAIN_CLIENT_ID = "clientId";
    /** Name of the budget under test. */
    private static final String BUDGET_NAME = "Fall semester";
    /** Name of the food category. */
    private static final String FOOD = "Food";
    /** Date shared by most purchases. */
    private static final String DAY_ONE = "2026-10-01";
    /** A ten dollar amount. */
    private static final String TEN = "10.00";
    /** A one hundred dollar amount. */
    private static final String HUNDRED = "100.00";
    /** Zero amount with two decimal places. */
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    /** Mock datastore used by the service. */
    @Mock
    private BudgetDataStore dataStore;

    /** Service under test. */
    private BudgetService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new BudgetService(dataStore);
        lenient().when(dataStore.withExclusiveLock(any()))
                .thenAnswer(invocation -> invocation
                        .<DataStoreOperation<?>>getArgument(0).execute());
    }

    private static BigDecimal money(final String value) {
        return new BigDecimal(value);
    }

    private static Category category(
            final String id,
            final String name,
            final String limit
    ) {
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

    private void givenClients() throws IOException {
        lenient().when(dataStore.<Client>readList(
                eq(BudgetService.CLIENTS_FILE), any()))
                .thenReturn(List.of(
                        new Client(CLIENT_ID, "Walmart", API_KEY),
                        new Client(OTHER_CLIENT_ID, "Amazon", OTHER_API_KEY)));
    }

    private void givenBudget() throws IOException {
        lenient().when(dataStore.<Budget>readList(
                eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(
                        new Budget(BUDGET_ID, BUDGET_NAME, CLIENT_ID)));
    }

    private void givenData(
            final List<Category> categories,
            final List<Purchase> purchases
    ) throws IOException {
        givenClients();
        givenBudget();
        when(dataStore.<Category>readList(
                eq(BudgetService.CATEGORIES_FILE), any()))
                .thenReturn(categories);
        when(dataStore.<Purchase>readList(
                eq(BudgetService.PURCHASES_FILE), any()))
                .thenReturn(purchases);
    }

    private BudgetReportResponse report() throws IOException {
        return service.getBudgetReport(BUDGET_ID, API_KEY).report();
    }

    // Access checks

    @Test
    void unknownApiKeyIsUnauthorized() throws IOException {
        givenClients();

        assertEquals(401,
                service.getBudgetReport(BUDGET_ID, "unknown-key").status(),
                "unknown API key should give 401");
    }

    @Test
    void emptyClientsFileIsUnauthorized() throws IOException {
        when(dataStore.<Client>readList(
                eq(BudgetService.CLIENTS_FILE), any())).thenReturn(null);

        assertEquals(401,
                service.getBudgetReport(BUDGET_ID, API_KEY).status(),
                "no clients means no valid key");
    }

    @Test
    void unknownBudgetIsNotFound() throws IOException {
        givenClients();
        when(dataStore.<Budget>readList(
                eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(
                        OTHER_BUDGET_ID, "Someone else", CLIENT_ID)));

        assertEquals(404,
                service.getBudgetReport(BUDGET_ID, API_KEY).status(),
                "missing budget should give 404");
    }

    @Test
    void emptyBudgetsFileIsNotFound() throws IOException {
        givenClients();
        when(dataStore.<Budget>readList(
                eq(BudgetService.BUDGETS_FILE), any())).thenReturn(null);

        assertEquals(404,
                service.getBudgetReport(BUDGET_ID, API_KEY).status(),
                "no budgets should give 404");
    }

    @Test
    void otherClientsBudgetIsForbidden() throws IOException {
        givenClients();
        givenBudget();

        assertEquals(403,
                service.getBudgetReport(BUDGET_ID, OTHER_API_KEY).status(),
                "another client's budget should give 403");
    }

    @Test
    void budgetWithNoOwnerIsForbidden() throws IOException {
        givenClients();
        when(dataStore.<Budget>readList(
                eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(BUDGET_ID, BUDGET_NAME, null)));

        assertEquals(403,
                service.getBudgetReport(BUDGET_ID, API_KEY).status(),
                "budget with no owner should give 403");
    }

    @Test
    void forbiddenRequestNeverReadsPurchases() throws IOException {
        givenClients();
        givenBudget();

        service.getBudgetReport(BUDGET_ID, OTHER_API_KEY);

        verify(dataStore, never()).readList(
                eq(BudgetService.PURCHASES_FILE), any());
    }

    @Test
    void ownerGetsReport() throws IOException {
        givenData(List.of(), List.of());

        assertEquals(200,
                service.getBudgetReport(BUDGET_ID, API_KEY).status(),
                "owner should get 200");
    }

    @Test
    void ownerMatchIgnoresCase() throws IOException {
        givenClients();
        when(dataStore.<Budget>readList(
                eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(new Budget(BUDGET_ID, BUDGET_NAME,
                        CLIENT_ID.toUpperCase(Locale.ROOT))));
        when(dataStore.<Category>readList(
                eq(BudgetService.CATEGORIES_FILE), any()))
                .thenReturn(List.of());
        when(dataStore.<Purchase>readList(
                eq(BudgetService.PURCHASES_FILE), any()))
                .thenReturn(List.of());

        assertEquals(200,
                service.getBudgetReport(BUDGET_ID, API_KEY).status(),
                "client ID match should ignore case");
    }

    // Report calculations

    @Test
    void budgetWithNoCategoriesHasZeroTotals() throws IOException {
        givenData(List.of(), List.of());
        final BudgetReportResponse report = report();

        assertAll(
                () -> assertEquals(BUDGET_ID, report.budgetId(), "budget id"),
                () -> assertEquals(BUDGET_NAME, report.budgetName(),
                        "budget name"),
                () -> assertEquals(ZERO, report.totalSpent(), "total spent"),
                () -> assertEquals(ZERO, report.totalLimit(), "total limit"),
                () -> assertNull(report.totalPercentUsed(),
                        "no limit means no percent"),
                () -> assertTrue(report.categories().isEmpty(),
                        "no category rows"),
                () -> assertTrue(report.purchases().isEmpty(),
                        "no purchase rows"));
    }

    @Test
    void categoryWithNoPurchasesShowsItsFullLimitRemaining()
            throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, "200.00")), List.of());
        final CategoryReport food = report().categories().get(0);

        assertAll(
                () -> assertEquals(ZERO, food.spent(), "nothing spent"),
                () -> assertEquals(money("200.00"), food.remaining(),
                        "full limit remains"),
                () -> assertEquals(ZERO, food.percentUsed(),
                        "zero percent used"));
    }

    @Test
    void computesCategoryAndOverallNumbers() throws IOException {
        givenData(
                List.of(category(FOOD_ID, FOOD, "200.00"),
                        category(RENT_ID, "Rent", "1000.00")),
                List.of(purchase("p1", FOOD_ID, "75.50", "2026-10-02"),
                        purchase("p2", RENT_ID, "1000.00", DAY_ONE)));
        final BudgetReportResponse report = report();
        final CategoryReport food = report.categories().get(0);
        final CategoryReport rent = report.categories().get(1);

        assertAll(
                () -> assertEquals(money("75.50"), food.spent(),
                        "food spent"),
                () -> assertEquals(money("124.50"), food.remaining(),
                        "food remaining"),
                () -> assertEquals(money("37.75"), food.percentUsed(),
                        "food percent used"),
                () -> assertEquals(ZERO, rent.remaining(), "rent remaining"),
                () -> assertEquals(money(HUNDRED), rent.percentUsed(),
                        "rent percent used"),
                () -> assertEquals(money("1200.00"), report.totalLimit(),
                        "total limit"),
                () -> assertEquals(money("1075.50"), report.totalSpent(),
                        "total spent"),
                () -> assertEquals(money("124.50"), report.totalRemaining(),
                        "total remaining"),
                () -> assertEquals(money("89.63"), report.totalPercentUsed(),
                        "total percent used"));
    }

    @Test
    void overspentCategoryHasNegativeRemaining() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, HUNDRED)),
                List.of(purchase("p1", FOOD_ID, "150.00", DAY_ONE)));
        final CategoryReport food = report().categories().get(0);

        assertAll(
                () -> assertEquals(money("-50.00"), food.remaining(),
                        "remaining goes negative"),
                () -> assertEquals(money("150.00"), food.percentUsed(),
                        "percent goes above 100"));
    }

    @Test
    void roundsPurchasesUpAndRemainingDown() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, TEN)),
                List.of(purchase("p1", FOOD_ID, "3.331", DAY_ONE)));
        final BudgetReportResponse report = report();

        assertAll(
                () -> assertEquals(money("3.34"),
                        report.categories().get(0).spent(),
                        "spent rounds up"),
                () -> assertEquals(money("6.66"),
                        report.categories().get(0).remaining(),
                        "remaining rounds down"),
                () -> assertEquals(money("3.34"),
                        report.purchases().get(0).amount(),
                        "purchase amount rounds up"));
    }

    @Test
    void ignoresOtherBudgetsAndOrphanPurchases() throws IOException {
        givenData(
                List.of(category(FOOD_ID, FOOD, HUNDRED),
                        new Category(OTHER_BUDGET_ID, OTHER_CAT_ID, FOOD,
                                money("500.00"))),
                List.of(purchase("mine", FOOD_ID, TEN, DAY_ONE),
                        purchase("theirs", OTHER_CAT_ID, "99.00", DAY_ONE),
                        purchase("orphan", "missing-category", "5.00",
                                DAY_ONE)));
        final BudgetReportResponse report = report();

        assertAll(
                () -> assertEquals(1, report.categories().size(),
                        "only this budget's categories"),
                () -> assertEquals(1, report.purchases().size(),
                        "only this budget's purchases"),
                () -> assertEquals("mine",
                        report.purchases().get(0).purchaseId(),
                        "kept purchase"),
                () -> assertEquals(money(TEN), report.totalSpent(),
                        "total counts only this budget"));
    }

    @Test
    void purchaseShowsItsCategoryName() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, HUNDRED)),
                List.of(purchase("p1", FOOD_ID, TEN, DAY_ONE)));

        assertEquals(FOOD, report().purchases().get(0).categoryName(),
                "purchase shows its category name");
    }

    @Test
    void purchasesAreSortedOldestFirst() throws IOException {
        givenData(List.of(category(FOOD_ID, FOOD, HUNDRED)),
                List.of(purchase("late", FOOD_ID, "1.00", "2026-10-20"),
                        purchase("early", FOOD_ID, "1.00", DAY_ONE)));

        assertEquals(List.of("early", "late"),
                report().purchases().stream()
                        .map(PurchaseSummary::purchaseId)
                        .toList(),
                "oldest purchase first");
    }

    @Test
    void nullCategoryAndPurchaseListsAreTreatedAsEmpty() throws IOException {
        givenData(null, null);
        final BudgetReportResponse report = report();

        assertAll(
                () -> assertTrue(report.categories().isEmpty(),
                        "null categories become empty"),
                () -> assertTrue(report.purchases().isEmpty(),
                        "null purchases become empty"));
    }

    @Test
    void readFailureIsPassedUp() throws IOException {
        when(dataStore.<Client>readList(
                eq(BudgetService.CLIENTS_FILE), any()))
                .thenThrow(new IOException("disk gone"));

        assertThrows(IOException.class,
                () -> service.getBudgetReport(BUDGET_ID, API_KEY),
                "read errors reach the controller");
    }

    @Test
    void roundingHelpersTreatNullAsZeroAndZeroLimitHasNoPercent() {
        assertAll(
                () -> assertEquals(ZERO, BudgetService.roundUp(null),
                        "null rounds up to zero"),
                () -> assertEquals(ZERO, BudgetService.roundDown(null),
                        "null rounds down to zero"),
                () -> assertNull(
                        BudgetService.percentUsed(ZERO, money("5.00")),
                        "zero limit has no percent"));
    }

    // createCategory ownership checks (from POST /budget/category)

    private void givenCategoryData(
            final String budgetOwner,
            final List<Category> categories
    ) throws IOException {
        when(dataStore.<Budget>readList(
                eq(BudgetService.BUDGETS_FILE), any()))
                .thenReturn(List.of(
                        new Budget(BUDGET_ID, BUDGET_NAME, budgetOwner)));
        when(dataStore.<String>readList(
                eq(BudgetService.CLIENTS_FILE), any()))
                .thenReturn(List.of(PLAIN_CLIENT_ID));
        when(dataStore.<Category>readList(
                eq(BudgetService.CATEGORIES_FILE), any()))
                .thenReturn(categories);
    }

    @Test
    void rejectsCategoryWhenBudgetBelongsToAnotherClient() throws IOException {
        givenCategoryData("other-client", List.of());

        final Map<String, Object> result = service.createCategory(
                PLAIN_CLIENT_ID, BUDGET_ID, FOOD, money(HUNDRED));

        assertAll(
                () -> assertEquals(false, result.get("success"),
                        "request fails"),
                () -> assertEquals(403, result.get("error"),
                        "status is 403"),
                () -> assertEquals("budgetId does not belong to clientId",
                        result.get("reason"), "reason explains why"));
    }

    @Test
    void rejectedCategoryIsNeverSaved() throws IOException {
        givenCategoryData("other-client", List.of());

        service.createCategory(
                PLAIN_CLIENT_ID, BUDGET_ID, FOOD, money(HUNDRED));

        verify(dataStore, never()).writeList(
                eq(BudgetService.CATEGORIES_FILE), any());
    }

    @Test
    void createsCategoryWhenBudgetBelongsToClient() throws IOException {
        givenCategoryData("CLIENTID", new ArrayList<>());

        final Map<String, Object> result = service.createCategory(
                PLAIN_CLIENT_ID, BUDGET_ID, FOOD, money(HUNDRED));

        assertAll(
                () -> assertEquals(true, result.get("success"),
                        "request succeeds"),
                () -> assertEquals(BUDGET_ID,
                        ((Category) result.get("returnValue")).getBudgetId(),
                        "category is in the requested budget"));
    }

    @Test
    void createdCategoryIsSaved() throws IOException {
        givenCategoryData("CLIENTID", new ArrayList<>());

        service.createCategory(
                PLAIN_CLIENT_ID, BUDGET_ID, FOOD, money(HUNDRED));

        verify(dataStore).writeList(eq(BudgetService.CATEGORIES_FILE), any());
    }
}
