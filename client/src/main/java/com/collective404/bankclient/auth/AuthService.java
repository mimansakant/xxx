package com.collective404.bankclient.auth;

import com.collective404.bankclient.model.AppUser;
import com.collective404.bankclient.repository.AppUserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessions;

    public AuthService(
            final AppUserRepository newUsers,
            final PasswordEncoder newPasswordEncoder,
            final SessionRegistry newSessions
    ) {
        users = newUsers;
        passwordEncoder = newPasswordEncoder;
        sessions = newSessions;
    }

    public AuthResult signUp(final String username, final String password) {
        final String normalized = normalizeUsername(username);
        if (password == null || password.length() < 10
                || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be between 10 and 128 characters");
        }
        if (users.existsByUsernameIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username is already registered");
        }
        final AppUser user;
        try {
            user = users.save(new AppUser(
                    normalized, passwordEncoder.encode(password)));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username is already registered", exception);
        }
        return new AuthResult(sessions.create(user.getId()), normalized);
    }

    public AuthResult signIn(final String username, final String password) {
        final String normalized = normalizeUsername(username);
        final AppUser user = users.findByUsernameIgnoreCase(normalized)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Username or password is incorrect"));
        if (password == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Username or password is incorrect");
        }
        return new AuthResult(sessions.create(user.getId()), normalized);
    }

    public String username(final Long userId) {
        return users.findById(userId)
                .map(AppUser::getUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    private String normalizeUsername(final String username) {
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username is required");
        }
        final String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9._-]{3,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username must be 3-64 letters, numbers, dots, underscores or hyphens");
        }
        return normalized;
    }

    public record AuthResult(String token, String username) {
    }
}
