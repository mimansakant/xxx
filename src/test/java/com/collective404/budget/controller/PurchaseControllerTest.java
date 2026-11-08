package com.collective404.budget.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.collective404.budget.dto.PurchaseDeleteResponse;
import com.collective404.budget.service.PurchaseService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP-level tests for {@code DELETE /purchases/{id}}.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseControllerTest {

    private static final String CLIENT_PARAM = "clientId";
    private static final String BUDGET_PARAM = "budgetId";

    private static final String CLIENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String BUDGET_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String CATEGORY_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String PURCHASE_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";
    private static final String DELETE_PATH = "/purchases/" + PURCHASE_ID;

    @Mock
    private PurchaseService purchaseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PurchaseController(purchaseService)).build();
    }

    private PurchaseDeleteResponse sampleResponse() {
        return new PurchaseDeleteResponse(
                PURCHASE_ID, new BigDecimal("20.00"), CATEGORY_ID,
                new BigDecimal("30.00"), new BigDecimal("70.00"));
    }

    @Test
    void deleteExistingPurchaseReturnsOk() throws Exception {
        when(purchaseService.deletePurchase(eq(CLIENT_ID), eq(BUDGET_ID), eq(PURCHASE_ID)))
                .thenReturn(sampleResponse());

        final MvcResult result = mockMvc.perform(delete(DELETE_PATH)
                .param(CLIENT_PARAM, CLIENT_ID)
                .param(BUDGET_PARAM, BUDGET_ID)).andReturn();

        assertEquals(200, result.getResponse().getStatus(),
                "deleting an existing purchase should return 200");
    }

    @Test
    void deleteResponseContainsRecalculatedTotals() throws Exception {
        when(purchaseService.deletePurchase(eq(CLIENT_ID), eq(BUDGET_ID), eq(PURCHASE_ID)))
                .thenReturn(sampleResponse());

        final MvcResult result = mockMvc.perform(delete(DELETE_PATH)
                .param(CLIENT_PARAM, CLIENT_ID)
                .param(BUDGET_PARAM, BUDGET_ID)).andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("amountRemaining"),
                "success body should include the recalculated totals");
    }

    @Test
    void deleteMissingPurchaseReturnsNotFound() throws Exception {
        when(purchaseService.deletePurchase(any(), any(), any())).thenReturn(null);

        final MvcResult result = mockMvc.perform(delete(DELETE_PATH)
                .param(CLIENT_PARAM, CLIENT_ID)
                .param(BUDGET_PARAM, BUDGET_ID)).andReturn();

        assertEquals(404, result.getResponse().getStatus(),
                "deleting an unknown purchase should return 404");
    }

    @Test
    void deleteWithInvalidPurchaseIdReturnsBadRequest() throws Exception {
        final MvcResult result = mockMvc.perform(delete("/purchases/not-a-uuid")
                .param(CLIENT_PARAM, CLIENT_ID)
                .param(BUDGET_PARAM, BUDGET_ID)).andReturn();

        assertEquals(400, result.getResponse().getStatus(),
                "a non-UUID purchase id should return 400");
    }

    @Test
    void deleteWithInvalidClientIdReturnsBadRequest() throws Exception {
        final MvcResult result = mockMvc.perform(delete(DELETE_PATH)
                .param(CLIENT_PARAM, "not-a-uuid")
                .param(BUDGET_PARAM, BUDGET_ID)).andReturn();

        assertEquals(400, result.getResponse().getStatus(),
                "a non-UUID clientId should return 400");
    }
}
