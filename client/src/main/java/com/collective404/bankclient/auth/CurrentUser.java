package com.collective404.bankclient.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

    private final SessionRegistry sessions;

    public CurrentUser(final SessionRegistry newSessions) {
        sessions = newSessions;
    }

    public Long requireId(final String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Sign in to continue");
        }
        final Long userId = sessions.getUserId(
                authorization.substring("Bearer ".length()).trim());
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Session expired; sign in again");
        }
        return userId;
    }

    public String tokenFrom(final String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
