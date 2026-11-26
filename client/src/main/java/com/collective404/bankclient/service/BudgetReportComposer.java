package com.collective404.bankclient.service;

import com.collective404.bankclient.dto.AccountBreakdown;
import com.collective404.bankclient.dto.BudgetReportView;
import com.collective404.bankclient.dto.CategoryView;
import com.collective404.bankclient.dto.PurchaseView;
import com.collective404.bankclient.model.ClientBudget;
import com.collective404.bankclient.model.PurchaseMetadata;
import com.collective404.bankclient.repository.PurchaseMetadataRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BudgetReportComposer {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetApiClient api;
    private final PurchaseMetadataRepository metadata;
    private final TrackedAccountService accounts;

    public BudgetReportComposer(
            final BudgetApiClient newApi,
            final PurchaseMetadataRepository newMetadata,
            final TrackedAccountService newAccounts
    ) {
        api = newApi;
        metadata = newMetadata;
        accounts = newAccounts;
    }

    public BudgetReportView compose(
            final Long userId,
            final ClientBudget budget,
            final String from,
            final String to,
            final String accountFilter,
            final String categoryFilter
    ) {
        final LocalDate fromDate = parseDate(from);
        final LocalDate toDate = parseDate(to);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date must not be after end date");
        }
        if (accountFilter != null) {
            accounts.requireOwned(userId, accountFilter);
            if (!accountFilter.equals(budget.getAccountId())) {
                return new BudgetReportView(
                        budget.getServiceBudgetId(),
                        budget.getName(),
                        budget.getAccountId(),
                        accounts.accountNameOrPersonal(
                                userId, budget.getAccountId()),
                        ZERO, ZERO, ZERO, null,
                        List.of(), List.of(), List.of());
            }
        }
        final BudgetApiClient.ServiceReport report =
                api.getReport(budget.getServiceBudgetId());
        final Map<String, BudgetApiClient.ServiceCategoryReport> categories =
                new HashMap<>();
        for (final BudgetApiClient.ServiceCategoryReport category
                : report.categories()) {
            categories.put(category.categoryId(), category);
        }
        final Map<String, PurchaseMetadata> localMetadata = new HashMap<>();
        for (final PurchaseMetadata item : metadata.findByBudgetIdAndOwnerId(
                budget.getServiceBudgetId(), userId)) {
            localMetadata.put(item.getServicePurchaseId(), item);
        }
        final List<PurchaseView> purchases = new ArrayList<>();
        for (final BudgetApiClient.ServicePurchaseSummary purchase
                : report.purchases()) {
            final PurchaseMetadata item = localMetadata.get(
                    purchase.purchaseId());
            final String categoryId = item != null
                    && item.getCategoryOverride() != null
                    ? item.getCategoryOverride() : purchase.categoryId();
            final BudgetApiClient.ServiceCategoryReport category =
                    categories.get(categoryId);
            final String categoryName = category == null
                    ? purchase.categoryName() : category.name();
            final String accountId = item == null
                    ? budget.getAccountId() : item.getAccountId();
            final PurchaseView view = new PurchaseView(
                    purchase.purchaseId(),
                    budget.getServiceBudgetId(),
                    budget.getName(),
                    accountId,
                    accounts.accountNameOrPersonal(userId, accountId),
                    categoryId,
                    categoryName,
                    item != null && item.getNameOverride() != null
                            ? item.getNameOverride() : purchase.name(),
                    item != null && item.getAmountOverride() != null
                            ? item.getAmountOverride() : purchase.amount(),
                    item != null && item.getDateOverride() != null
                            ? item.getDateOverride() : purchase.date(),
                    item != null && item.isCategorySuggested());
            if (matches(view, fromDate, toDate, accountFilter, categoryFilter)) {
                purchases.add(view);
            }
        }

        BigDecimal totalLimit = ZERO;
        BigDecimal totalSpent = ZERO;
        final Map<String, BigDecimal> spentByCategory = new HashMap<>();
        final Map<String, BigDecimal> spentByAccount = new HashMap<>();
        for (final BudgetApiClient.ServiceCategoryReport category
                : report.categories()) {
            if (categoryFilter == null
                    || category.categoryId().equals(categoryFilter)) {
                totalLimit = totalLimit.add(category.limit());
                spentByCategory.put(category.categoryId(), ZERO);
            }
        }
        for (final PurchaseView purchase : purchases) {
            totalSpent = totalSpent.add(purchase.amount());
            if (spentByCategory.containsKey(purchase.categoryId())) {
                spentByCategory.merge(purchase.categoryId(),
                        purchase.amount(), BigDecimal::add);
            }
            spentByAccount.merge(
                    accountKey(purchase.accountId()),
                    purchase.amount(), BigDecimal::add);
        }
        final List<CategoryView> categoryViews = report.categories().stream()
                .filter(category -> categoryFilter == null
                        || category.categoryId().equals(categoryFilter))
                .map(category -> {
                    final BigDecimal spent = spentByCategory.getOrDefault(
                            category.categoryId(), ZERO);
                    return new CategoryView(
                            category.categoryId(),
                            category.name(),
                            category.limit(),
                            spent.setScale(2, RoundingMode.HALF_UP),
                            category.limit().subtract(spent)
                                    .setScale(2, RoundingMode.HALF_UP),
                            percent(spent, category.limit()));
                })
                .toList();
        final List<AccountBreakdown> accountBreakdown = spentByAccount
                .entrySet().stream()
                .map(entry -> {
                    final String id = "personal".equals(entry.getKey())
                            ? null : entry.getKey();
                    return new AccountBreakdown(
                            id,
                            accounts.accountNameOrPersonal(userId, id),
                            entry.getValue().setScale(2, RoundingMode.HALF_UP));
                })
                .toList();
        return new BudgetReportView(
                report.budgetId(),
                budget.getName(),
                budget.getAccountId(),
                accounts.accountNameOrPersonal(
                        userId, budget.getAccountId()),
                totalLimit.setScale(2, RoundingMode.HALF_UP),
                totalSpent.setScale(2, RoundingMode.HALF_UP),
                totalLimit.subtract(totalSpent).setScale(
                        2, RoundingMode.HALF_UP),
                percent(totalSpent, totalLimit),
                categoryViews,
                accountBreakdown,
                purchases);
    }

    private static boolean matches(
            final PurchaseView purchase,
            final LocalDate from,
            final LocalDate to,
            final String accountId,
            final String categoryId
    ) {
        if (accountId != null && !accountId.equals(purchase.accountId())) {
            return false;
        }
        if (categoryId != null && !categoryId.equals(purchase.categoryId())) {
            return false;
        }
        final LocalDate date = parseDate(purchase.date());
        return date != null && (from == null || !date.isBefore(from))
                && (to == null || !date.isAfter(to));
    }

    private static LocalDate parseDate(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dates must use YYYY-MM-DD format");
        }
    }

    private static BigDecimal percent(
            final BigDecimal amount, final BigDecimal limit) {
        if (limit.signum() <= 0) {
            return null;
        }
        return amount.multiply(HUNDRED)
                .divide(limit, 2, RoundingMode.HALF_UP);
    }

    private static String accountKey(final String accountId) {
        return accountId == null ? "personal" : accountId;
    }
}
