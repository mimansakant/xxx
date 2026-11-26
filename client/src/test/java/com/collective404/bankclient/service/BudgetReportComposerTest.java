package com.collective404.bankclient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.collective404.bankclient.dto.BudgetReportView;
import com.collective404.bankclient.model.ClientBudget;
import com.collective404.bankclient.model.PurchaseMetadata;
import com.collective404.bankclient.model.TrackedAccount;
import com.collective404.bankclient.repository.ClientBudgetRepository;
import com.collective404.bankclient.repository.PurchaseMetadataRepository;
import com.collective404.bankclient.repository.TrackedAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bankclient-report;DB_CLOSE_DELAY=-1"
})
class BudgetReportComposerTest {

    private static final String BUDGET_ID =
            "d56b0c19-b806-459b-b7f5-e7de5ee6529a";
    private static final String ACCOUNT_ID =
            "f6d74a4e-6ca2-47a9-9b2b-a5d221d1fced";

    @Autowired
    private PurchaseMetadataRepository metadata;

    @Autowired
    private TrackedAccountRepository accounts;

    @Autowired
    private ClientBudgetRepository budgets;

    @BeforeEach
    void clearData() {
        metadata.deleteAll();
        budgets.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void dateFilterUsesLocallyEditedPurchaseFieldsAndRebuildsTotals() {
        final BudgetApiClient.ServiceReport report =
                new BudgetApiClient.ServiceReport(
                        BUDGET_ID, "Monthly", new BigDecimal("200.00"),
                        new BigDecimal("35.00"), new BigDecimal("165.00"),
                        new BigDecimal("17.50"),
                        List.of(category("food", "Food"),
                                category("travel", "Travel")),
                        List.of(
                                purchase("coffee-1", "food", "Cafe", "15.00",
                                        "2026-03-03"),
                                purchase("train-1", "travel", "Train", "20.00",
                                        "2026-03-10")));
        accounts.save(new TrackedAccount(
                ACCOUNT_ID, 42L, "Main account", "checking", null));
        final PurchaseMetadata local = new PurchaseMetadata(
                "coffee-1", 42L, BUDGET_ID, ACCOUNT_ID);
        local.setCategoryOverride("travel");
        local.setNameOverride("Coffee updated");
        local.setAmountOverride(new BigDecimal("8.00"));
        local.setDateOverride("2026-03-05");
        metadata.save(local);

        final BudgetApiClient api = new FixedBudgetApiClient(report);
        final BudgetReportComposer composer = new BudgetReportComposer(
                api, metadata,
                new TrackedAccountService(accounts, budgets));
        final ClientBudget budget =
                new ClientBudget(BUDGET_ID, 42L, "Monthly", ACCOUNT_ID);

        final BudgetReportView result = composer.compose(
                42L, budget, "2026-03-04", "2026-03-06",
                ACCOUNT_ID, null);

        assertEquals(1, result.purchases().size());
        assertEquals("Coffee updated", result.purchases().get(0).name());
        assertEquals(new BigDecimal("8.00"), result.totalSpent());
        assertEquals(new BigDecimal("0.00"),
                result.categories().get(0).spent());
        assertEquals(new BigDecimal("8.00"),
                result.categories().get(1).spent());
    }

    private static BudgetApiClient.ServiceCategoryReport category(
            final String id, final String name) {
        return new BudgetApiClient.ServiceCategoryReport(
                id, name, new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("100.00"), BigDecimal.ZERO);
    }

    private static BudgetApiClient.ServicePurchaseSummary purchase(
            final String id,
            final String categoryId,
            final String name,
            final String amount,
            final String date) {
        return new BudgetApiClient.ServicePurchaseSummary(
                id, categoryId, name, name,
                new BigDecimal(amount), date);
    }

    private static final class FixedBudgetApiClient extends BudgetApiClient {

        private final ServiceReport report;

        FixedBudgetApiClient(final ServiceReport newReport) {
            super(RestClient.builder().build(), "http://localhost:8080",
                    "test-key", "test-client");
            report = newReport;
        }

        @Override
        public ServiceReport getReport(final String budgetId) {
            return report;
        }
    }
}
