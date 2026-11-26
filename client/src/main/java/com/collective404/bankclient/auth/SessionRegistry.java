package com.collective404.bankclient.auth;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {

    private static final long SESSION_SECONDS = 12 * 60 * 60;
    private final ConcurrentHashMap<String, Session> sessions =
            new ConcurrentHashMap<>();

    public String create(final Long userId) {
        final String token = UUID.randomUUID().toString()
                + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId,
                Instant.now().plusSeconds(SESSION_SECONDS)));
        return token;
    }

    public Long getUserId(final String token) {
        final Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return session.userId();
    }

    public void remove(final String token) {
        sessions.remove(token);
    }

    private record Session(Long userId, Instant expiresAt) {
    }
}
