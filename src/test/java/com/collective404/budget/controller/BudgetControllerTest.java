package com.collective404.budget.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collective404.budget.dto.BudgetReportResponse;
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

    private static final String BUDGET_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private BudgetService budgetService;

    private BudgetController controller;

    @BeforeEach
    void setUp() {
        controller = new BudgetController(budgetService);
    }

    @Test
    void returns200WithReport() throws IOException {
        final BudgetReportResponse report = new BudgetReportResponse(BUDGET_ID, "Fall semester",
                new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("150.00"),
                new BigDecimal("25.00"), List.of(), List.of());
        when(budgetService.getBudgetReport(BUDGET_ID)).thenReturn(report);

        final ResponseEntity<?> response = controller.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "status 200"),
                () -> assertSame(report, response.getBody(), "body is the report"));
    }

    @Test
    void returns404WhenBudgetMissing() throws IOException {
        when(budgetService.getBudgetReport(BUDGET_ID)).thenReturn(null);

        final ResponseEntity<?> response = controller.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "status 404"),
                () -> assertEquals(404, ((Map<?, ?>) response.getBody()).get("status"), "404 in body"));
    }

    @Test
    void returns400ForNonUuidId() throws IOException {
        final ResponseEntity<?> response = controller.getBudgetReport("not-a-uuid");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "status 400");
    }

    @Test
    void badIdNeverReachesService() throws IOException {
        controller.getBudgetReport("not-a-uuid");

        verify(budgetService, never()).getBudgetReport(anyString());
    }

    @Test
    void returns500WhenDataUnavailable() throws IOException {
        when(budgetService.getBudgetReport(BUDGET_ID)).thenThrow(new IOException("disk gone"));

        final ResponseEntity<?> response = controller.getBudgetReport(BUDGET_ID);

        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "status 500"),
                () -> assertEquals(500, ((Map<?, ?>) response.getBody()).get("status"), "500 in body"));
    }

    @Test
    void uuidCheckRejectsBadInput() {
        assertAll(
                () -> assertTrue(BudgetController.isValidUuid(BUDGET_ID), "real UUID passes"),
                () -> assertFalse(BudgetController.isValidUuid(null), "null fails"),
                () -> assertFalse(BudgetController.isValidUuid("  "), "blank fails"),
                () -> assertFalse(BudgetController.isValidUuid("1-1-1-1-1"), "short form fails"));
    }
}
