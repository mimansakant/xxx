package com.collective404.bankclient.repository;

import com.collective404.bankclient.model.ClientBudget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientBudgetRepository
        extends JpaRepository<ClientBudget, String> {
    List<ClientBudget> findByOwnerIdOrderByNameAsc(Long ownerId);
    Optional<ClientBudget> findByServiceBudgetIdAndOwnerId(
            String serviceBudgetId, Long ownerId);
    boolean existsByServiceBudgetIdAndOwnerId(
            String serviceBudgetId, Long ownerId);
    boolean existsByAccountIdAndOwnerId(String accountId, Long ownerId);
}
