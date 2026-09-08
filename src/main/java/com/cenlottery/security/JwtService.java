package com.cenlottery.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Issues and verifies JWTs via jjwt (HMAC-SHA256). If no secret is configured (app.jwt.secret /
 * JWT_SECRET env var) a random one is generated at startup, exactly like the previous
 * hand-rolled implementation: sessions won't survive a process restart, which is fine for this
 * TF validation build, but a fixed secret should be set for a real deployment with multiple
 * instances or planned restarts.
 */
@Component
public class JwtService {
    private static final Logger LOG = Logger.getLogger("JwtService");

    private final SecretKey key;
    private final long expiryMs;

    public JwtService(@Value("${app.jwt.secret:}") String configuredSecret,
                       @Value("${app.jwt.expiry-ms:43200000}") long expiryMs) {
        this.expiryMs = expiryMs;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            LOG.warning("app.jwt.secret not set - generating a random signing key for this process. " +
                    "Existing sessions will be invalidated on restart.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(
                    normalizeToBase64(configuredSecret)));
        }
    }

    private static String normalizeToBase64(String secret) {
        // Accept either a raw passphrase or an already-base64 secret; pad/derive 32 bytes either way.
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return Base64.getEncoder().encodeToString(raw);
        }
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, raw.length);
        return Base64.getEncoder().encodeToString(padded);
    }

    public String issue(String userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiryMs);
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public record TokenPayload(String userId, String username) {
    }

    /** Returns null if the token is missing, malformed, has a bad signature, or is expired. */
    public TokenPayload verify(String token) {
        if (token == null) return null;
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenPayload(claims.getSubject(), claims.get("username", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
