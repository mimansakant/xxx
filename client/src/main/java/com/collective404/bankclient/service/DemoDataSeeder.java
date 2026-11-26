package com.collective404.bankclient.service;

import com.collective404.bankclient.model.AppUser;
import com.collective404.bankclient.model.ClientBudget;
import com.collective404.bankclient.repository.AppUserRepository;
import com.collective404.bankclient.repository.ClientBudgetRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "bank.client.demo-seed",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DemoDataSeeder implements ApplicationRunner {

    private static final List<DemoUser> DEMO_USERS = List.of(
            new DemoUser("harbor.alex", "HarborDemo!2026", List.of(
                    new DemoBudget(
                            "2bbe5a18-6630-4e5b-b212-5e0a8d33c728",
                            "Monthly household"),
                    new DemoBudget(
                            "31da0fdc-ddf6-4355-96e7-327cdccc3328",
                            "Monthly household"),
                    new DemoBudget(
                            "1de35b2c-2274-46ed-8fcd-8b573028171a",
                            "Home improvement"),
                    new DemoBudget(
                            "e6634b23-345a-41b7-96cf-d79fa4f95c27",
                            "Dining out"))),
            new DemoUser("harbor.jordan", "BudgetDemo#2026", List.of(
                    new DemoBudget(
                            "209c48b4-a5a7-4efb-9d1c-d13ed57899f5",
                            "Student expenses"),
                    new DemoBudget(
                            "b88b3b43-8fb2-420e-9247-f2f0b88ba053",
                            "Dining out"),
                    new DemoBudget(
                            "44fd6fc2-4e25-447d-8ba2-388ceddbc394",
                            "Dining out"))),
            new DemoUser("harbor.taylor", "SavingsDemo!26", List.of(
                    new DemoBudget(
                            "133c44cd-3dd4-4c44-bea9-4fcf8d9570d9",
                            "Travel fund"),
                    new DemoBudget(
                            "1ba7db38-a069-4563-a0f0-e6e598952a06",
                            "Travel fund"),
                    new DemoBudget(
                            "dc1f7894-dc7c-4dd3-8e5c-5b3476340763",
                            "Auto expenses"))));

    private final AppUserRepository users;
    private final ClientBudgetRepository budgets;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            final AppUserRepository newUsers,
            final ClientBudgetRepository newBudgets,
            final PasswordEncoder newPasswordEncoder
    ) {
        users = newUsers;
        budgets = newBudgets;
        passwordEncoder = newPasswordEncoder;
    }

    @Override
    public void run(final ApplicationArguments args) {
        for (final DemoUser demoUser : DEMO_USERS) {
            final AppUser user = users
                    .findByUsernameIgnoreCase(demoUser.username())
                    .orElseGet(() -> users.save(new AppUser(
                            demoUser.username(),
                            passwordEncoder.encode(demoUser.password()))));
            for (final DemoBudget demoBudget : demoUser.budgets()) {
                if (!budgets.existsByServiceBudgetIdAndOwnerId(
                        demoBudget.id(), user.getId())) {
                    budgets.save(new ClientBudget(
                            demoBudget.id(), user.getId(), demoBudget.name(),
                            null));
                }
            }
        }
    }

    private record DemoUser(
            String username, String password, List<DemoBudget> budgets) {
    }

    private record DemoBudget(String id, String name) {
    }
}
