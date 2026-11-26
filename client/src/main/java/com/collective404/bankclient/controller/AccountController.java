package com.collective404.bankclient.controller;

import com.collective404.bankclient.auth.CurrentUser;
import com.collective404.bankclient.dto.AccountRequest;
import com.collective404.bankclient.dto.AccountResponse;
import com.collective404.bankclient.service.TrackedAccountService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final CurrentUser currentUser;
    private final TrackedAccountService accounts;

    public AccountController(
            final CurrentUser newCurrentUser,
            final TrackedAccountService newAccounts
    ) {
        currentUser = newCurrentUser;
        accounts = newAccounts;
    }

    @GetMapping
    public List<AccountResponse> list(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization) {
        return accounts.list(currentUser.requireId(authorization));
    }

    @PostMapping
    public AccountResponse create(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @RequestBody final AccountRequest request) {
        return accounts.create(currentUser.requireId(authorization), request);
    }

    @DeleteMapping("/{accountId}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization,
            @PathVariable final String accountId) {
        accounts.delete(currentUser.requireId(authorization), accountId);
    }
}
