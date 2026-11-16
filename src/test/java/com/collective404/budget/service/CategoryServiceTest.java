package com.collective404.budget.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;
import com.collective404.budget.store.BudgetDataStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * File-backed tests for category creation and persistence.
 */
class CategoryServiceTest {

    /** JSON mapper used to verify persisted data. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Temporary directory used by the file-backed datastore. */
    @TempDir
    private Path tempDir;

    /** Service under test. */
    private BudgetService service;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(tempDir.resolve("clients.json"), "[\"client-1\"]");
        Files.writeString(
                tempDir.resolve("budgets.json"),
                "[{\"id\":\"budget-1\",\"name\":\"Budget One\","
                        + "\"clientId\":\"client-1\"},"
                        + "{\"id\":\"budget-2\",\"name\":\"Budget Two\","
                        + "\"clientId\":\"client-1\"}]"
        );
        Files.writeString(tempDir.resolve("categories.json"), "[]");
        service = new BudgetService(new BudgetDataStore(objectMapper, tempDir));
    }

    @Test
    void createsAndPersistsCategoryWithRoundedLimit() throws Exception {
        final Map<String, Object> result = service.createCategory(
                "client-1", "budget-1", "Groceries", new BigDecimal("23.459"));

        assertEquals(true, result.get("success"));
        final Category category = (Category) result.get("returnValue");
        assertNotNull(category);
        assertEquals("budget-1", category.getBudgetId());
        assertEquals("Groceries", category.getName());
        assertEquals(new BigDecimal("23.45"), category.getLimit());
        assertEquals(category.getId(), readCategories().get(0).getId());
    }

    @Test
    void rejectsCaseInsensitiveDuplicateButAllowsSameNameInAnotherBudget()
            throws Exception {
        service.createCategory(
                "client-1", "budget-1", "Groceries", new BigDecimal("20.00"));

        final Map<String, Object> duplicate = service.createCategory(
                "client-1", "budget-1", "gROCERIES", new BigDecimal("30.00"));
        final Map<String, Object> otherBudget = service.createCategory(
                "client-1", "budget-2", "Groceries", new BigDecimal("30.00"));

        assertEquals(409, duplicate.get("error"));
        assertEquals(true, otherBudget.get("success"));
        assertEquals(2, readCategories().size());
    }

    @Test
    void createsBudgetWhenBudgetIdIsOmitted() throws Exception {
        final Map<String, Object> result = service.createCategory(
                "client-1", null, "Utilities", new BigDecimal("75.00"));
        final Category category = (Category) result.get("returnValue");
        final List<Budget> savedBudgets = readBudgets();

        assertEquals(true, result.get("success"));
        assertEquals(3, savedBudgets.size());
        assertTrue(savedBudgets.stream().anyMatch(budget ->
                budget.getId().equals(category.getBudgetId())));
        assertEquals(category.getId(), readCategories().get(0).getId());
    }

    @Test
    void rejectsUnknownClientBudgetAndInvalidValues() throws Exception {
        assertEquals(404, service.createCategory(
                "missing-client", "budget-1", "Food",
                new BigDecimal("20.00")).get("error"));
        assertEquals(404, service.createCategory(
                "client-1", "missing-budget", "Food",
                new BigDecimal("20.00")).get("error"));
        assertEquals(400, service.createCategory(
                null, "budget-1", "Food",
                new BigDecimal("20.00")).get("error"));
        assertEquals(400, service.createCategory(
                "client-1", "budget-1", "123!@#",
                new BigDecimal("20.00")).get("error"));
        assertEquals(400, service.createCategory(
                "client-1", "budget-1", "Food", BigDecimal.ZERO).get("error"));
        assertEquals(false, service.createCategory(
                "client-1", "budget-1", "Food",
                new BigDecimal("-1.00")).get("success"));
        assertTrue(readCategories().isEmpty());
    }

    @Test
    void rejectsBudgetOwnedByAnotherClientWithoutCreatingCategory()
            throws Exception {
        Files.writeString(
                tempDir.resolve("budgets.json"),
                "[{\"id\":\"budget-1\",\"name\":\"Budget One\","
                        + "\"clientId\":\"another-client\"}]"
        );

        final Map<String, Object> result = service.createCategory(
                "client-1", "budget-1", "Food", new BigDecimal("20.00"));

        assertEquals(false, result.get("success"));
        assertEquals(403, result.get("error"));
        assertEquals(
                "budgetId does not belong to clientId", result.get("reason"));
        assertTrue(readCategories().isEmpty());
    }

    @Test
    void concurrentRequestsForSameCategoryCreateOnlyOne() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch start = new CountDownLatch(1);
        try {
            final Future<Map<String, Object>> first = executor.submit(() -> {
                start.await();
                return service.createCategory(
                        "client-1", "budget-1", "Groceries",
                        new BigDecimal("20.00"));
            });
            final Future<Map<String, Object>> second = executor.submit(() -> {
                start.await();
                return service.createCategory(
                        "client-1", "budget-1", "groceries",
                        new BigDecimal("25.00"));
            });

            start.countDown();
            final List<Map<String, Object>> results =
                    List.of(first.get(), second.get());
            assertEquals(1, results.stream()
                    .filter(result -> Boolean.TRUE.equals(
                            result.get("success"))).count());
            assertEquals(1, results.stream()
                    .filter(result -> Integer.valueOf(409).equals(
                            result.get("error"))).count());
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
