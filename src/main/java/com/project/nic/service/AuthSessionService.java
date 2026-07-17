package com.project.nic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.nic.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthSessionService {
    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] jwtSecret;

    public AuthSessionService(ObjectMapper objectMapper, @Value("${app.auth.jwt.secret}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("app.auth.jwt.secret must be configured");
        }
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String createSession(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(SESSION_TTL_SECONDS);
        String role = user.getRole() == null || user.getRole().isBlank() ? "CITIZEN" : user.getRole().toUpperCase();

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", role);
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encode(header);
        String encodedClaims = encode(claims);
        String signedContent = encodedHeader + "." + encodedClaims;
        return signedContent + "." + sign(signedContent);
    }

    public Optional<SessionUser> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String normalizedToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        String[] parts = normalizedToken.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String signedContent = parts[0] + "." + parts[1];
        if (!signatureMatches(parts[2], sign(signedContent))) {
            return Optional.empty();
        }

        try {
            Map<String, Object> claims = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), MAP_TYPE);
            Instant expiresAt = Instant.ofEpochSecond(asLong(claims.get("exp")));
            if (expiresAt.isBefore(Instant.now())) {
                return Optional.empty();
            }

            Long userId = asLong(claims.get("sub"));
            String email = String.valueOf(claims.get("email"));
            String role = String.valueOf(claims.get("role"));
            return Optional.of(new SessionUser(userId, email, role, expiresAt));
        } catch (Exception e) {
            return Optional.empty();
        }
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

    private String encode(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode JWT value", e);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtSecret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private boolean signatureMatches(String providedSignature, String expectedSignature) {
        return MessageDigest.isEqual(
                providedSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
