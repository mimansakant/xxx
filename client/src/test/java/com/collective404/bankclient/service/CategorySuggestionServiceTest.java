package com.collective404.bankclient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorySuggestionServiceTest {

    private final CategorySuggestionService service =
            new CategorySuggestionService();

    @Test
    void suggestsMatchingCategoryFromPurchaseDescription() {
        final var suggestion = service.suggest("Cafe Rivera latte",
                List.of(category("food", "Food"),
                        category("dining", "Dining out"),
                        category("transport", "Transportation")));

        assertEquals("dining", suggestion.id());
    }

    @Test
    void usesMiscCategoryForUnrecognizedPurchase() {
        final var suggestion = service.suggest("Unknown merchant",
                List.of(category("food", "Food"),
                        category("misc", "Miscellaneous")));

        assertEquals("misc", suggestion.id());
    }

    private static BudgetApiClient.ServiceCategoryReport category(
            final String id, final String name) {
        return new BudgetApiClient.ServiceCategoryReport(
                id, name, new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("100.00"), BigDecimal.ZERO);
    }
}
