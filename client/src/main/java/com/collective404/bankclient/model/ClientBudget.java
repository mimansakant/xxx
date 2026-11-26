package com.collective404.bankclient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_budgets")
public class ClientBudget {

    @Id
    @Column(length = 36)
    private String serviceBudgetId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 36)
    private String accountId;

    protected ClientBudget() {
    }

    public ClientBudget(
            final String newServiceBudgetId,
            final Long newOwnerId,
            final String newName,
            final String newAccountId
    ) {
        serviceBudgetId = newServiceBudgetId;
        ownerId = newOwnerId;
        name = newName;
        accountId = newAccountId;
    }

    public String getServiceBudgetId() {
        return serviceBudgetId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getAccountId() {
        return accountId;
    }
}
