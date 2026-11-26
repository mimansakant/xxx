package com.collective404.bankclient.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CategorySuggestionService {

    private static final List<KeywordGroup> KEYWORD_GROUPS = List.of(
            new KeywordGroup(List.of("grocery", "groceries", "market",
                    "supermarket", "food", "produce"), List.of("groceries",
                    "grocery", "food")),
            new KeywordGroup(List.of("restaurant", "cafe", "coffee",
                    "diner", "takeout", "pizza", "delivery"), List.of(
                    "dining", "restaurant", "food")),
            new KeywordGroup(List.of("gas", "fuel", "parking", "transit",
                    "uber", "lyft", "taxi", "bus", "train"), List.of(
                    "transport", "transit", "gas", "fuel")),
            new KeywordGroup(List.of("rent", "mortgage", "landlord"),
                    List.of("housing", "rent")),
            new KeywordGroup(List.of("electric", "water", "internet",
                    "utility", "utilities", "phone"), List.of(
                    "utilities", "bills")),
            new KeywordGroup(List.of("pharmacy", "doctor", "medical",
                    "clinic", "dentist"), List.of("health", "medical")),
            new KeywordGroup(List.of("movie", "cinema", "concert",
                    "netflix", "spotify", "theatre"), List.of(
                    "entertainment", "subscriptions")),
            new KeywordGroup(List.of("hotel", "airline", "flight",
                    "airbnb", "travel"), List.of("travel", "vacation")),
            new KeywordGroup(List.of("amazon", "clothing", "retail",
                    "store"), List.of("shopping", "retail"))
    );

    public ServiceApiCategory suggest(
            final String details,
            final List<BudgetApiClient.ServiceCategoryReport> categories
    ) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        final String text = " " + details.toLowerCase(Locale.ROOT) + " ";
        for (final KeywordGroup group : KEYWORD_GROUPS) {
            if (group.keywords().stream()
                    .anyMatch(keyword -> containsWord(text, keyword))) {
                final ServiceApiCategory match = findCategory(
                        categories, group.categoryNames());
                if (match != null) {
                    return match;
                }
            }
        }
        final ServiceApiCategory misc = findCategory(
                categories, List.of("misc", "other", "uncategorized"));
        return misc == null ? fromReport(categories.get(0)) : misc;
    }

    public ServiceApiCategory findById(
            final String categoryId,
            final List<BudgetApiClient.ServiceCategoryReport> categories
    ) {
        return categories.stream()
                .filter(category -> category.categoryId().equals(categoryId))
                .map(CategorySuggestionService::fromReport)
                .findFirst()
                .orElse(null);
    }

    private ServiceApiCategory findCategory(
            final List<BudgetApiClient.ServiceCategoryReport> categories,
            final List<String> names
    ) {
        for (final String name : names) {
            for (final BudgetApiClient.ServiceCategoryReport category
                    : categories) {
                if (category.name().toLowerCase(Locale.ROOT).contains(name)) {
                    return fromReport(category);
                }
            }
        }
        return null;
    }

    private static boolean containsWord(
            final String text, final String keyword) {
        return Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b")
                .matcher(text)
                .find();
    }

    private static ServiceApiCategory fromReport(
            final BudgetApiClient.ServiceCategoryReport category) {
        return new ServiceApiCategory(
                category.categoryId(), category.name());
    }

    private record KeywordGroup(
            List<String> keywords, List<String> categoryNames) {
    }

    public record ServiceApiCategory(String id, String name) {
    }
}
