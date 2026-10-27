
package com.collective404.budget.controller;

import com.collective404.budget.service.BudgetService;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the budget controller.
 */
class CategoryUpdateControllerTest {

    private static final String ENDPOINT = "/budget/category";

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(tempDir.resolve("clients.json"), """
            ["client-1"]
            """);

        Files.writeString(tempDir.resolve("budgets.json"), """
            [
                {"id": "budget-1", "name": "Budget One",
                "clientId": "client-1"}
            ]
            """);

        Files.writeString(tempDir.resolve("categories.json"), """
            [
                {
                    "budgetId": "budget-1",
                    "id": "category-1",
                    "name": "Groceries",
                    "limit": 200.00
                },
                {
                    "budgetId": "budget-1",
                    "id": "category-2",
                    "name": "Rent",
                    "limit": 1000.00
                }
            ]
            """);

        Files.writeString(tempDir.resolve("purchases.json"), "[]");

        BudgetDataStore dataStore = new BudgetDataStore(
            new ObjectMapper(), tempDir);

        mockMvc = MockMvcBuilders.standaloneSetup(
            new BudgetController(new BudgetService(dataStore))).build();
    }

    @Test
    void updateCategoryReturnsOk() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "category-1",
                        "name": "Food",
                        "limit": 150.00
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("category-1"))
            .andExpect(jsonPath("$.name").value("Food"))
            .andExpect(jsonPath("$.limit").value(150.00));
    }

    @Test
    void updateCategoryNameOnly() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "category-1",
                        "name": "Food"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Food"))
            .andExpect(jsonPath("$.limit").value(200.00));
    }

    @Test
    void updateCategoryLimitOnly() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "category-1",
                        "limit": 150.00
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Groceries"))
            .andExpect(jsonPath("$.limit").value(150.00));
    }

    @Test
    void updateCategoryReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "category-1"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(400));
    }

    @Test
    void updateCategoryReturnsNotFound() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "missing-category",
                        "name": "Food"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(404));
    }

    @Test
    void updateCategoryReturnsConflict() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "category-1",
                        "name": "rEnT"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(409))
            .andExpect(jsonPath("$.existingCategoryId").value("category-2"));
    }

    @Test
    void updateCategoryReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategoryReturns500WhenDataUnavailable() throws Exception {
        Files.delete(tempDir.resolve("purchases.json"));

        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-1",
                        "budgetId": "budget-1",
                        "categoryId": "category-1",
                        "name": "Food"
                    }
                    """))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void updateCategoryRejectsWrongClient() throws Exception {
        Files.writeString(tempDir.resolve("clients.json"),
            "[\"client-1\", \"client-2\"]");

        mockMvc.perform(patch(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "client-2",
                        "budgetId": "budget-1",
                        "categoryId": "category-1",
                        "name": "Food"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(404));
    }
}
