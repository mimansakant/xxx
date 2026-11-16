package com.collective404.budget.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collective404.budget.dto.BudgetReportResponse;
import com.collective404.budget.dto.CategoryRequest;
import com.collective404.budget.service.BudgetReportResult;
import com.collective404.budget.service.BudgetService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Status-code tests for GET /budget/{budgetId}. The service is mocked.
 */
@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    /** A valid budget identifier used in requests. */
    private static final String BUDGET_ID =
            "11111111-1111-1111-1111-111111111111";

    /** A client's API key used in requests. */
    private static final String API_KEY =
            "66666666-6666-6666-6666-666666666666";

    /** A budget ID that isn't a UUID. */
    private static final String BAD_ID = "not-a-uuid";

    /** Key of the status number in error bodies. */
    private static final String STATUS = "status";

    /** Mock budget service used by the controller. */
    @Mock
    private BudgetService budgetService;

    /** Controller under test. */
    private BudgetController controller;

    /** Creates the controller before each test. */
    @BeforeEach
    void setUp() {
        controller = new BudgetController(budgetService);
    }

    private static Object bodyStatus(final ResponseEntity<?> response) {
        return ((Map<?, ?>) response.getBody()).get(STATUS);
    }

    @Test
    void returns200WithReport() throws IOException {
        final BudgetReportResponse report = new BudgetReportResponse(
                BUDGET_ID, "Fall semester", new BigDecimal("200.00"),
                new BigDecimal("50.00"), new BigDecimal("150.00"),
                new BigDecimal("25.00"), List.of(), List.of());
        when(budgetService.getBudgetReport(BUDGET_ID, API_KEY))
                .thenReturn(BudgetReportResult.ok(report));

        final ResponseEntity<?> response =
                controller.getBudgetReport(BUDGET_ID, API_KEY);

        assertAll(
                () -> assertEquals(HttpStatus.OK, response.getStatusCode(),
                        "status 200"),
                () -> assertSame(report, response.getBody(),
                        "body is the report"));
    }

    @Test
    void trimsApiKeyBeforeLookup() throws IOException {
        when(budgetService.getBudgetReport(BUDGET_ID, API_KEY))
                .thenReturn(BudgetReportResult.ok(null));

        assertEquals(HttpStatus.OK,
                controller.getBudgetReport(BUDGET_ID, " " + API_KEY + " ")
                        .getStatusCode(),
                "surrounding spaces are ignored");
    }

    @Test
    void returns400ForNonUuidId() throws IOException {
        final ResponseEntity<?> response =
                controller.getBudgetReport(BAD_ID, API_KEY);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "status 400");
    }

    @Test
    void badIdNeverReachesService() throws IOException {
        controller.getBudgetReport(BAD_ID, API_KEY);

        verify(budgetService, never()).getBudgetReport(anyString(),
                anyString());
    }

    @Test
    void returns401WhenApiKeyMissing() throws IOException {
        final ResponseEntity<?> response =
                controller.getBudgetReport(BUDGET_ID, null);

        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED,
                        response.getStatusCode(), "status 401"),
                () -> assertEquals(401, bodyStatus(response), "401 in body"));
    }

    @Test
    void returns401WhenApiKeyBlank() throws IOException {
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.getBudgetReport(BUDGET_ID, "   ").getStatusCode(),
                "blank key gives 401");
    }

    @Test
    void missingKeyNeverReachesService() throws IOException {
        controller.getBudgetReport(BUDGET_ID, null);

        verify(budgetService, never()).getBudgetReport(anyString(),
                anyString());
    }

    @Test
    void passesServiceRefusalThrough() throws IOException {
        when(budgetService.getBudgetReport(BUDGET_ID, API_KEY))
                .thenReturn(BudgetReportResult.error(
                        BudgetReportResult.FORBIDDEN, "not yours"));

        final ResponseEntity<?> response =
                controller.getBudgetReport(BUDGET_ID, API_KEY);

        assertAll(
                () -> assertEquals(HttpStatus.FORBIDDEN,
                        response.getStatusCode(), "status 403"),
                () -> assertEquals(403, bodyStatus(response), "403 in body"),
                () -> assertEquals("not yours",
                        ((Map<?, ?>) response.getBody()).get("reason"),
                        "reason from the service"));
    }

    @Test
    void returns404WhenBudgetMissing() throws IOException {
        when(budgetService.getBudgetReport(BUDGET_ID, API_KEY))
                .thenReturn(BudgetReportResult.error(
                        BudgetReportResult.NOT_FOUND, "no budget"));

        assertEquals(HttpStatus.NOT_FOUND,
                controller.getBudgetReport(BUDGET_ID, API_KEY)
                        .getStatusCode(),
                "status 404");
    }

    @Test
    void returns500WhenDataUnavailable() throws IOException {
        when(budgetService.getBudgetReport(BUDGET_ID, API_KEY))
                .thenThrow(new IOException("disk gone"));

        final ResponseEntity<?> response =
                controller.getBudgetReport(BUDGET_ID, API_KEY);

        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                        response.getStatusCode(), "status 500"),
                () -> assertEquals(500, bodyStatus(response), "500 in body"));
    }

    @Test
    void returnsForbiddenWhenBudgetDoesNotBelongToClient() throws IOException {
        final Map<String, Object> error = Map.of(
                "success", false,
                "error", 403,
                "reason", "budgetId does not belong to clientId");
        when(budgetService.createCategory(
                "clientId", BUDGET_ID, "Food", new BigDecimal("100.00")))
                .thenReturn(error);

        final ResponseEntity<?> response = controller.createCategory(
                new CategoryRequest(
                        "clientId", BUDGET_ID, "Food",
                        new BigDecimal("100.00")));

        assertAll(
                () -> assertEquals(HttpStatus.FORBIDDEN,
                        response.getStatusCode(), "status 403"),
                () -> assertSame(error, response.getBody(),
                        "body is the service error"));
    }
}
