package com.collective404.bankclient.repository;

import com.collective404.bankclient.model.PurchaseMetadata;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseMetadataRepository
        extends JpaRepository<PurchaseMetadata, String> {
    List<PurchaseMetadata> findByBudgetIdAndOwnerId(
            String budgetId, Long ownerId);
    List<PurchaseMetadata> findByOwnerId(Long ownerId);
    Optional<PurchaseMetadata> findByServicePurchaseIdAndOwnerId(
            String servicePurchaseId, Long ownerId);
}
