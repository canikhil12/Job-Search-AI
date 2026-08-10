package com.jobmatch.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates stateless HS256 access tokens.
 *
 * <p>The signing key is derived from the {@code JWT_SECRET} environment variable.
 * HS256 requires a key of at least 256 bits (32 bytes); a shorter secret makes JJWT
 * throw {@code WeakKeyException}. We fail fast at construction so a misconfigured
 * deployment never starts serving traffic with a weak MAC.</p>
 */
@Service
public class JwtService {

    /** Access-token lifetime. Refresh tokens are a later phase. */
    public static final Duration TOKEN_TTL = Duration.ofMinutes(60);

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Provide a secret of at least " + MIN_SECRET_BYTES + " characters.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) for HS256; got "
                            + keyBytes.length + " bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Issues a signed token whose subject is the user id. Returns the token and its expiry. */
    public IssuedToken issueToken(String subject) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_TTL);
        String token = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Parses and verifies the token, returning the subject (user id). Throws on any invalid token. */
    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
