package com.collective404.bankclient.service;

import com.collective404.bankclient.dto.AccountRequest;
import com.collective404.bankclient.dto.AccountResponse;
import com.collective404.bankclient.model.TrackedAccount;
import com.collective404.bankclient.repository.ClientBudgetRepository;
import com.collective404.bankclient.repository.TrackedAccountRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrackedAccountService {

    private static final List<String> ACCOUNT_TYPES = List.of(
            "checking", "savings", "credit", "debit", "other");

    private final TrackedAccountRepository accounts;
    private final ClientBudgetRepository budgets;

    public TrackedAccountService(
            final TrackedAccountRepository newAccounts,
            final ClientBudgetRepository newBudgets
    ) {
        accounts = newAccounts;
        budgets = newBudgets;
    }

    public List<AccountResponse> list(final Long userId) {
        return accounts.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(TrackedAccountService::toResponse)
                .toList();
    }

    public AccountResponse create(
            final Long userId, final AccountRequest request) {
        if (request == null || request.name() == null
                || request.name().isBlank() || request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account name and type are required");
        }
        final String type = request.type().trim().toLowerCase(Locale.ROOT);
        if (!ACCOUNT_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account type must be checking, savings, credit, debit or other");
        }
        final String name = request.name().trim();
        if (name.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account name must be at most 80 characters");
        }
        final String institution = request.institution() == null
                || request.institution().isBlank()
                ? null : request.institution().trim();
        if (institution != null && institution.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Institution must be at most 80 characters");
        }
        return toResponse(accounts.save(new TrackedAccount(
                UUID.randomUUID().toString(), userId, name, type, institution)));
    }

    public void delete(final Long userId, final String accountId) {
        final TrackedAccount account = accounts.findByIdAndOwnerId(
                accountId, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found"));
        if (budgets.existsByAccountIdAndOwnerId(accountId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Move or remove this account's budgets before deleting it");
        }
        accounts.delete(account);
    }

    public TrackedAccount requireOwned(
            final Long userId, final String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        return accounts.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));
    }

    public String accountName(final Long userId, final String accountId) {
        if (accountId == null) {
            return "Personal";
        }
        return requireOwned(userId, accountId).getName();
    }

    public String accountNameOrPersonal(
            final Long userId, final String accountId) {
        return accountName(userId, accountId);
    }

    private static AccountResponse toResponse(final TrackedAccount account) {
        return new AccountResponse(
                account.getId(), account.getName(), account.getType(),
                account.getInstitution());
    }
}
