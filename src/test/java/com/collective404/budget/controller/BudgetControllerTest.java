package com.collective404.budget.controller;

import com.collective404.budget.service.BudgetService;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetControllerTest {

    private static final String CATEGORY_ENDPOINT = "/v1/budget/category";

    private MockMvc mockMvc;
    private BudgetDataStore dataStore;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(tempDir.resolve("clients.json"), "[\"client-1\"]");
        Files.writeString(
            tempDir.resolve("budgets.json"),
            "[{\"id\":\"budget-1\",\"name\":\"Budget One\"},"
                + "{\"id\":\"budget-2\",\"name\":\"Budget Two\"}]"
        );
        Files.writeString(tempDir.resolve("categories.json"), "[]");
        dataStore = new BudgetDataStore(new ObjectMapper(), tempDir);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new BudgetController(new BudgetService(dataStore))
        ).build();
    }

    @Test
    void createCategoryReturnsCreatedCategoryWithInitialAmounts() throws Exception {
        mockMvc.perform(post(CATEGORY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clientId": "client-1",
                      "budgetId": "budget-1",
                      "name": "Groceries",
                      "limit": 125.50
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Groceries"))
            .andExpect(jsonPath("$.limit").value(125.50))
            .andExpect(jsonPath("$.amountSpent").value(0.00))
            .andExpect(jsonPath("$.remainingAmount").value(125.50));
    }

    @Test
    void createCategoryReturnsBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post(CATEGORY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clientId": "client-1",
                      "budgetId": "budget-1",
                      "name": "123!@#",
                      "limit": 125.50
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(400))
            .andExpect(jsonPath("$.reason").value("invalid category name"));
    }

    @Test
    void createCategoryReturnsNotFoundForUnknownBudget() throws Exception {
        mockMvc.perform(post(CATEGORY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clientId": "client-1",
                      "budgetId": "missing-budget",
                      "name": "Groceries",
                      "limit": 125.50
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(404))
            .andExpect(jsonPath("$.reason").value("budgetId not found"));
    }

    @Test
    void createCategoryReturnsConflictIncludingExistingCategoryId() throws Exception {
        final String request = """
            {
              "clientId": "client-1",
              "budgetId": "budget-1",
              "name": "Groceries",
              "limit": 125.50
            }
            """;
        mockMvc.perform(post(CATEGORY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated());
        mockMvc.perform(post(CATEGORY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(409))
            .andExpect(jsonPath("$.existingCategoryId").isNotEmpty());
    }

    @Test
    void createCategoryReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post(CATEGORY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest());
    }

}
