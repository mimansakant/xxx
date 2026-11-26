package com.collective404.bankclient.controller;

import com.collective404.bankclient.auth.AuthService;
import com.collective404.bankclient.auth.CurrentUser;
import com.collective404.bankclient.auth.SessionRegistry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final CurrentUser currentUser;
    private final SessionRegistry sessions;

    public AuthController(
            final AuthService newAuth,
            final CurrentUser newCurrentUser,
            final SessionRegistry newSessions
    ) {
        auth = newAuth;
        currentUser = newCurrentUser;
        sessions = newSessions;
    }

    @PostMapping("/signup")
    public AuthService.AuthResult signUp(
            @RequestBody final Credentials credentials) {
        return auth.signUp(credentials.username(), credentials.password());
    }

    @PostMapping("/login")
    public AuthService.AuthResult signIn(
            @RequestBody final Credentials credentials) {
        return auth.signIn(credentials.username(), credentials.password());
    }

    @GetMapping("/me")
    public Profile profile(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization) {
        final Long userId = currentUser.requireId(authorization);
        return new Profile(auth.username(userId));
    }

    @PostMapping("/logout")
    public void signOut(
            @RequestHeader(value = "Authorization", required = false)
            final String authorization) {
        currentUser.requireId(authorization);
        sessions.remove(currentUser.tokenFrom(authorization));
    }

    public record Credentials(String username, String password) {
    }

    public record Profile(String username) {
    }
}
