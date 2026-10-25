package com.collective404.budget.service;

import com.collective404.budget.model.Category;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for BudgetService.
 */
class BudgetServiceTest {

    private BudgetService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {

        Files.writeString(tempDir.resolve("clients.json"), "[\"client-1\"]");

        Files.writeString(tempDir.resolve("budgets.json"),
            """
            [
                {
                    "id": "budget-1",
                    "name": "Budget One"
                }
            ]
            """
        );

        Files.writeString(tempDir.resolve("categories.json"),
            """
            [
                {
                    "budgetId": "budget-1",
                    "id": "category-1",
                    "name": "Groceries",
                    "limit": 200.00
                }
            ]
            """
        );

        BudgetDataStore dataStore = new BudgetDataStore(new ObjectMapper(), tempDir);

        service = new BudgetService(dataStore);
    }

    @Test
    void updatesExistingCategory() throws Exception {

        Map<String, Object> result = service.updateCategory(
            "client-1",
            "budget-1",
            "category-1",
            "Food",
            new BigDecimal("150.00")
        );

        assertTrue((Boolean) result.get("success"));

        Category updated = (Category) result.get("returnValue");

        assertEquals("category-1", updated.getId());
        assertEquals("Food", updated.getName());
        assertEquals(new BigDecimal("150.00"), updated.getLimit());

        ObjectMapper mapper = new ObjectMapper();

        List<Category> savedCategories = mapper.readValue(tempDir.resolve("categories.json").toFile(), new TypeReference<List<Category>>() { });

        assertEquals(1, savedCategories.size());
        assertEquals("Food", savedCategories.get(0).getName());
        assertEquals(new BigDecimal("150.00"), savedCategories.get(0).getLimit()
        );
    }

    @Test
    void updatesNameOnly() throws Exception {
        Map<String, Object> result = service.updateCategory("client-1", "budget-1", "category-1", "Food", null);

        assertTrue((Boolean) result.get("success"));

        Category updated = (Category) result.get("returnValue");
        assertEquals("Food", updated.getName());
        assertEquals(new BigDecimal("200.00"), updated.getLimit());
    }

    @Test
    void updatesLimitOnly() throws Exception {
        Map<String, Object> result = service.updateCategory("client-1", "budget-1", "category-1", null, new BigDecimal("150.00"));

        assertTrue((Boolean) result.get("success"));

        Category updated = (Category) result.get("returnValue");
        assertEquals("Groceries", updated.getName());
        assertEquals(new BigDecimal("150.00"), updated.getLimit());
    }

    @Test
    void rejectsNoUpdateFields() throws Exception {
        Map<String, Object> result = service.updateCategory("client-1", "budget-1", "category-1", null, null);

        assertEquals(false, result.get("success"));
        assertEquals(400, result.get("error"));
        assertEquals("no update fields provided", result.get("reason"));
    }

    @Test
    void rejectsInvalidName() throws Exception {
        Map<String, Object> result = service.updateCategory(
            "client-1", "budget-1", "category-1", "123!@#", null);

        assertEquals(false, result.get("success"));
        assertEquals(400, result.get("error"));
        assertEquals("invalid category name", result.get("reason"));
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        Map<String, Object> result = service.updateCategory(
            "client-1", "budget-1", "category-1", null,
            new BigDecimal("-50.00"));

        assertEquals(false, result.get("success"));
        assertEquals(400, result.get("error"));
        assertEquals("invalid limit", result.get("reason"));
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        Map<String, Object> result = service.updateCategory(
            "client-1", "budget-1", "missing-category", "Food", null);

        assertEquals(false, result.get("success"));
        assertEquals(404, result.get("error"));
        assertEquals("categoryId not found", result.get("reason"));
    }

    @Test
    void rejectsDuplicateName() throws Exception {
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

        Map<String, Object> result = service.updateCategory(
            "client-1", "budget-1", "category-1", "rEnT", null);

        assertEquals(false, result.get("success"));
        assertEquals(409, result.get("error"));
        assertEquals("category-2", result.get("existingCategoryId"));
    }


}
