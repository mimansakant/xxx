package com.collective404.bankclient.service;

import com.collective404.bankclient.dto.BudgetCreateRequest;
import com.collective404.bankclient.dto.BudgetReportView;
import com.collective404.bankclient.dto.BudgetSummary;
import com.collective404.bankclient.dto.CategoryRequest;
import com.collective404.bankclient.dto.PurchaseCreateRequest;
import com.collective404.bankclient.dto.PurchaseUpdateRequest;
import com.collective404.bankclient.dto.PurchaseView;
import com.collective404.bankclient.model.ClientBudget;
import com.collective404.bankclient.model.PurchaseMetadata;
import com.collective404.bankclient.repository.ClientBudgetRepository;
import com.collective404.bankclient.repository.PurchaseMetadataRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientBudgetService {

    private final ClientBudgetRepository budgets;
    private final PurchaseMetadataRepository metadata;
    private final BudgetApiClient api;
    private final BudgetReportComposer reports;
    private final TrackedAccountService accounts;
    private final CategorySuggestionService categorizer;

    public ClientBudgetService(
            final ClientBudgetRepository newBudgets,
            final PurchaseMetadataRepository newMetadata,
            final BudgetApiClient newApi,
            final BudgetReportComposer newReports,
            final TrackedAccountService newAccounts,
            final CategorySuggestionService newCategorizer
    ) {
        budgets = newBudgets;
        metadata = newMetadata;
        api = newApi;
        reports = newReports;
        accounts = newAccounts;
        categorizer = newCategorizer;
    }

    @Transactional
    public BudgetReportView create(
            final Long userId, final BudgetCreateRequest request) {
        if (request == null || request.name() == null
                || request.name().isBlank()
                || request.firstCategory() == null
                || request.firstCategory().isBlank()
                || !validAmount(request.firstCategoryLimit())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Budget name, first category and positive limit are required");
        }
        final String accountId = blankToNull(request.accountId());
        accounts.requireOwned(userId, accountId);
        final String name = request.name().trim();
        if (name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Budget name must be at most 100 characters");
        }
        final BudgetApiClient.ServiceCategory created =
                api.createCategory(null, name, request.firstCategory().trim(),
                        request.firstCategoryLimit());
        if (created == null || created.budgetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Budget service did not return the new budget ID");
        }
        final ClientBudget budget = budgets.save(new ClientBudget(
                created.budgetId(), userId, name, accountId));
        return reports.compose(userId, budget, null, null, null, null);
    }

    public List<BudgetSummary> list(final Long userId) {
        return budgets.findByOwnerIdOrderByNameAsc(userId).stream()
                .map(budget -> {
                    final BudgetReportView report = reports.compose(
                            userId, budget, null, null, null, null);
                    return new BudgetSummary(
                            budget.getServiceBudgetId(),
                            budget.getName(),
                            budget.getAccountId(),
                            report.accountName(),
                            report.totalLimit(),
                            report.totalSpent(),
                            report.totalRemaining());
                })
                .toList();
    }

    public BudgetReportView report(
            final Long userId,
            final String budgetId,
            final String from,
            final String to,
            final String accountId,
            final String categoryId
    ) {
        return reports.compose(
                userId, requireOwnedBudget(userId, budgetId),
                blankToNull(from), blankToNull(to),
                blankToNull(accountId), blankToNull(categoryId));
    }

    @Transactional
    public BudgetReportView createCategory(
            final Long userId,
            final String budgetId,
            final CategoryRequest request
    ) {
        final ClientBudget budget = requireOwnedBudget(userId, budgetId);
        if (request == null || request.name() == null
                || request.name().isBlank()
                || !validAmount(request.limit())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category name and positive limit are required");
        }
        api.createCategory(
                budgetId, null, request.name().trim(), request.limit());
        return reports.compose(userId, budget, null, null, null, null);
    }

    @Transactional
    public BudgetReportView updateCategory(
            final Long userId,
            final String budgetId,
            final String categoryId,
            final CategoryRequest request
    ) {
        final ClientBudget budget = requireOwnedBudget(userId, budgetId);
        if (request == null
                || (isBlank(request.name()) && request.limit() == null)
                || (request.limit() != null && !validAmount(request.limit()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide a category name or a positive limit");
        }
        api.updateCategory(
                budgetId, categoryId,
                blankToNull(request.name()), request.limit());
        return reports.compose(userId, budget, null, null, null, null);
    }

    @Transactional
    public PurchaseView createPurchase(
            final Long userId,
            final String budgetId,
            final PurchaseCreateRequest request
    ) {
        final ClientBudget budget = requireOwnedBudget(userId, budgetId);
        if (request == null || request.name() == null
                || request.name().isBlank()
                || request.name().trim().length() > 200
                || !validAmount(request.amount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase name (up to 200 characters) and positive amount are required");
        }
        validateDate(request.date());
        final BudgetApiClient.ServiceReport report =
                api.getReport(budgetId);
        if (report.categories().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Add a budget category before recording a purchase");
        }
        final CategorySuggestionService.ServiceApiCategory selected;
        final boolean suggested;
        if (isBlank(request.categoryId())) {
            selected = categorizer.suggest(
                    request.name(), report.categories());
            suggested = true;
        } else {
            selected = categorizer.findById(
                    request.categoryId(), report.categories());
            suggested = false;
            if (selected == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected category does not belong to this budget");
            }
        }
        final BudgetApiClient.ServicePurchase created =
                api.createPurchase(budgetId, request.name().trim(),
                        request.amount(), selected.name());
        final PurchaseMetadata item = new PurchaseMetadata(
                created.id(), userId, budgetId, budget.getAccountId());
        item.setCategorySuggested(suggested);
        if (!isBlank(request.date())) {
            item.setDateOverride(request.date());
        }
        metadata.save(item);
        return findPurchase(userId, created.id()).purchaseView();
    }

    public List<PurchaseView> listPurchases(
            final Long userId,
            final String from,
            final String to,
            final String accountId,
            final String budgetId,
            final String categoryId
    ) {
        validateDateRange(from, to);
        if (accountId != null) {
            accounts.requireOwned(userId, accountId);
        }
        final String normalizedAccountId = blankToNull(accountId);
        final String normalizedBudgetId = blankToNull(budgetId);
        final List<ClientBudget> selectedBudgets = normalizedBudgetId == null
                ? budgets.findByOwnerIdOrderByNameAsc(userId)
                : List.of(requireOwnedBudget(userId, normalizedBudgetId));
        return selectedBudgets.stream()
                .flatMap(budget -> reports.compose(
                        userId, budget, from, to, normalizedAccountId,
                        blankToNull(categoryId))
                        .purchases().stream())
                .sorted(Comparator.comparing(
                        PurchaseView::date,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public PurchaseView updatePurchase(
            final Long userId,
            final String purchaseId,
            final PurchaseUpdateRequest request
    ) {
        if (request == null || (isBlank(request.name())
                && request.amount() == null && isBlank(request.date())
                && isBlank(request.categoryId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide at least one purchase field to update");
        }
        if (request.name() != null && request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase name cannot be blank");
        }
        if (request.name() != null && request.name().trim().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase name must be at most 200 characters");
        }
        if (request.amount() != null && !validAmount(request.amount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase amount must be positive with at most 2 decimals");
        }
        validateDate(request.date());
        final PurchaseContext context = findPurchase(userId, purchaseId);
        final PurchaseMetadata item = context.metadata();
        if (item.getAccountId() != null
                && (request.name() != null || request.amount() != null
                    || !isBlank(request.date()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Details of account-linked purchases cannot be changed");
        }
        if (!isBlank(request.categoryId())) {
            final BudgetApiClient.ServiceReport report =
                    api.getReport(context.budget().getServiceBudgetId());
            if (categorizer.findById(
                    request.categoryId(), report.categories()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected category does not belong to this budget");
            }
            item.setCategoryOverride(request.categoryId());
            item.setCategorySuggested(false);
        }
        if (request.name() != null) {
            item.setNameOverride(request.name().trim());
        }
        if (request.amount() != null) {
            item.setAmountOverride(request.amount());
        }
        if (!isBlank(request.date())) {
            item.setDateOverride(request.date());
        }
        metadata.save(item);
        return findPurchase(userId, purchaseId).purchaseView();
    }

    @Transactional
    public void deletePurchase(final Long userId, final String purchaseId) {
        final PurchaseContext context = findPurchase(userId, purchaseId);
        api.deletePurchase(
                purchaseId, context.budget().getServiceBudgetId());
        metadata.delete(context.metadata());
    }

    private PurchaseContext findPurchase(
            final Long userId, final String purchaseId) {
        final PurchaseMetadata known = metadata
                .findByServicePurchaseIdAndOwnerId(purchaseId, userId)
                .orElse(null);
        if (known != null) {
            final ClientBudget budget = requireOwnedBudget(
                    userId, known.getBudgetId());
            return purchaseContext(userId, budget, purchaseId, known);
        }
        for (final ClientBudget budget
                : budgets.findByOwnerIdOrderByNameAsc(userId)) {
            final BudgetApiClient.ServiceReport report =
                    api.getReport(budget.getServiceBudgetId());
            final boolean found = report.purchases().stream()
                    .anyMatch(item -> item.purchaseId().equals(purchaseId));
            if (found) {
                final PurchaseMetadata item = metadata.save(
                        new PurchaseMetadata(purchaseId, userId,
                                budget.getServiceBudgetId(),
                                budget.getAccountId()));
                return purchaseContext(userId, budget, purchaseId, item);
            }
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Purchase not found");
    }

    private PurchaseContext purchaseContext(
            final Long userId,
            final ClientBudget budget,
            final String purchaseId,
            final PurchaseMetadata item
    ) {
        final BudgetReportView report = reports.compose(
                userId, budget, null, null, null, null);
        final PurchaseView view = report.purchases().stream()
                .filter(purchase -> purchase.purchaseId().equals(purchaseId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Purchase not found"));
        return new PurchaseContext(budget, item, view);
    }

    private ClientBudget requireOwnedBudget(
            final Long userId, final String budgetId) {
        return budgets.findByServiceBudgetIdAndOwnerId(budgetId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private static boolean validAmount(final BigDecimal amount) {
        return amount != null && amount.signum() > 0
                && amount.scale() <= 2;
    }

    private static void validateDate(final String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date must use YYYY-MM-DD format");
        }
    }

    private static void validateDateRange(
            final String from, final String to) {
        validateDate(from);
        validateDate(to);
        if (!isBlank(from) && !isBlank(to)
                && LocalDate.parse(from).isAfter(LocalDate.parse(to))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date must not be after end date");
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(final String value) {
        return isBlank(value) ? null : value.trim();
    }

    private record PurchaseContext(
            ClientBudget budget,
            PurchaseMetadata metadata,
            PurchaseView purchaseView
    ) {
    }
}
