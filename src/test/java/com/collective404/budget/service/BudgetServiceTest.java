
package com.collective404.budget.service;

import com.collective404.budget.model.Budget;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private BudgetDataStore dataStore;
    private BudgetService service;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(tempDir.resolve("clients.json"), "[\"client-1\"]");
        Files.writeString(
            tempDir.resolve("budgets.json"),
            "[{\"id\":\"budget-1\",\"name\":\"Budget One\"},"
                + "{\"id\":\"budget-2\",\"name\":\"Budget Two\"}]"
        );
        Files.writeString(tempDir.resolve("categories.json"), "[]");
        dataStore = new BudgetDataStore(objectMapper, tempDir);
        service = new BudgetService(dataStore);
    }

    @Test
    void createsAndPersistsCategoryWithRoundedLimit() throws Exception {
        Map<String, Object> result = service.createCategory(
            "client-1",
            "budget-1",
            "Groceries",
            new BigDecimal("23.459")
        );

        assertEquals(true, result.get("success"));
        Category category = (Category) result.get("returnValue");
        assertNotNull(category);
        assertEquals("budget-1", category.getBudgetId());
        assertEquals("Groceries", category.getName());
        assertEquals(new BigDecimal("23.45"), category.getLimit());

        List<Category> savedCategories = readCategories();
        assertEquals(1, savedCategories.size());
        assertEquals(category.getId(), savedCategories.get(0).getId());
        assertEquals(new BigDecimal("23.45"), savedCategories.get(0).getLimit());
    }

    @Test
    void rejectsCaseInsensitiveDuplicateInSameBudgetButAllowsOtherBudget()
            throws Exception {
        assertEquals(true, service.createCategory(
            "client-1",
            "budget-1",
            "Groceries",
            new BigDecimal("20.00")
        ).get("success"));

        Map<String, Object> duplicateResult = service.createCategory(
            "client-1",
            "budget-1",
            "gROCERIES",
            new BigDecimal("30.00")
        );
        assertEquals(false, duplicateResult.get("success"));
        assertEquals(409, duplicateResult.get("error"));
        assertEquals(1, readCategories().size());

        Map<String, Object> otherBudgetResult = service.createCategory(
            "client-1",
            "budget-2",
            "Groceries",
            new BigDecimal("30.00")
        );
        assertEquals(true, otherBudgetResult.get("success"));
        assertEquals(2, readCategories().size());
    }

    @Test
    void createsBudgetWhenBudgetIdIsOmitted() throws Exception {
        Map<String, Object> result = service.createCategory(
            "client-1",
            null,
            "Utilities",
            new BigDecimal("75.00")
        );

        assertEquals(true, result.get("success"));
        Category category = (Category) result.get("returnValue");
        List<Budget> savedBudgets = readBudgets();
        assertEquals(3, savedBudgets.size());
        assertTrue(savedBudgets.stream().anyMatch(budget ->
            budget.getId().equals(category.getBudgetId())
        ));
        assertEquals(category.getId(), readCategories().get(0).getId());
    }

    @Test
    void rejectsUnknownClientAndBudget() throws Exception {
        Map<String, Object> unknownClient = service.createCategory(
            "missing-client",
            "budget-1",
            "Groceries",
            new BigDecimal("20.00")
        );
        assertEquals(404, unknownClient.get("error"));

        Map<String, Object> unknownBudget = service.createCategory(
            "client-1",
            "missing-budget",
            "Groceries",
            new BigDecimal("20.00")
        );
        assertEquals(404, unknownBudget.get("error"));
        assertTrue(readCategories().isEmpty());
    }

    @Test
    void rejectsInvalidRequiredValuesAndNonPositiveLimits() throws Exception {
        assertEquals(400, service.createCategory(
            null,
            "budget-1",
            "Groceries",
            new BigDecimal("20.00")
        ).get("error"));
        assertEquals(400, service.createCategory(
            "client-1",
            "budget-1",
            "123!@#",
            new BigDecimal("20.00")
        ).get("error"));
        assertEquals(400, service.createCategory(
            "client-1",
            "budget-1",
            "Groceries",
            BigDecimal.ZERO
        ).get("error"));
        assertFalse(Boolean.TRUE.equals(service.createCategory(
            "client-1",
            "budget-1",
            "Groceries",
            new BigDecimal("-1.00")
        ).get("success")));
        assertTrue(readCategories().isEmpty());
    }

    @Test
    void concurrentRequestsForSameCategoryCreateOnlyOne() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Map<String, Object>> first = executor.submit(() -> {
                start.await();
                return service.createCategory(
                    "client-1",
                    "budget-1",
                    "Groceries",
                    new BigDecimal("20.00")
                );
            });
            Future<Map<String, Object>> second = executor.submit(() -> {
                start.await();
                return service.createCategory(
                    "client-1",
                    "budget-1",
                    "groceries",
                    new BigDecimal("25.00")
                );
            });

            start.countDown();
            List<Map<String, Object>> results = List.of(first.get(), second.get());
            assertEquals(1, results.stream()
                .filter(result -> Boolean.TRUE.equals(result.get("success")))
                .count());
            assertEquals(1, results.stream()
                .filter(result -> Integer.valueOf(409).equals(result.get("error")))
                .count());
            assertEquals(1, readCategories().size());
        } finally {
            executor.shutdownNow();
        }
    }

    private List<Category> readCategories() throws Exception {
        return objectMapper.readValue(
            tempDir.resolve("categories.json").toFile(),
            new TypeReference<>() { }
        );
    }

    private List<Budget> readBudgets() throws Exception {
        return objectMapper.readValue(
            tempDir.resolve("budgets.json").toFile(),
            new TypeReference<>() { }
        );
    }
}
