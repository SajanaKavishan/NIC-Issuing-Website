package com.project.nic.service;

import com.project.nic.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {
    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60;

    private final Map<String, SessionUser> sessions = new ConcurrentHashMap<>();

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionUser(
                user.getId(),
                user.getEmail(),
                user.getRole() == null || user.getRole().isBlank() ? "CITIZEN" : user.getRole(),
                Instant.now().plusSeconds(SESSION_TTL_SECONDS)
        ));
        return token;
    }

    public Optional<SessionUser> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessionUser session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public boolean hasRole(String token, String requiredRole) {
        return findByToken(token)
                .map(session -> requiredRole.equalsIgnoreCase(session.role()))
                .orElse(false);
    }

    public boolean hasAnyRole(String token, String... requiredRoles) {
        return findByToken(token)
                .map(session -> {
                    for (String role : requiredRoles) {
                        if (role.equalsIgnoreCase(session.role())) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    public record SessionUser(Long userId, String email, String role, Instant expiresAt) {
    }
}
