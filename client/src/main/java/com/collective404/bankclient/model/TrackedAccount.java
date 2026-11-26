package com.collective404.bankclient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracked_accounts")
public class TrackedAccount {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 80)
    private String institution;

    @Column(nullable = false)
    private Instant createdAt;

    protected TrackedAccount() {
    }

    public TrackedAccount(
            final String newId,
            final Long newOwnerId,
            final String newName,
            final String newType,
            final String newInstitution
    ) {
        id = newId;
        ownerId = newOwnerId;
        name = newName;
        type = newType;
        institution = newInstitution;
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getInstitution() {
        return institution;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
