package com.collective404.bankclient.repository;

import com.collective404.bankclient.model.TrackedAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedAccountRepository
        extends JpaRepository<TrackedAccount, String> {
    List<TrackedAccount> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<TrackedAccount> findByIdAndOwnerId(String id, Long ownerId);
}
