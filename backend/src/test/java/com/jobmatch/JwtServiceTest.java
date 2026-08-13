package com.jobmatch;

import com.jobmatch.auth.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void issuesTokenThatRoundTripsToTheSameSubject() {
        String subject = UUID.randomUUID().toString();

        JwtService.IssuedToken issued = jwtService.issueToken(subject);

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());
        assertThat(jwtService.extractSubject(issued.token())).isEqualTo(subject);
    }

    @Test
    void rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder()
                .subject("someone")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.extractSubject(expired))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.issueToken("subject").token();
        // Flip the FIRST character of the signature. (The LAST base64url char of an HS512
        // signature encodes mostly padding bits, so flipping it can leave the decoded signature
        // — and thus validity — unchanged; the first char is always fully significant.)
        int sig = token.lastIndexOf('.') + 1;
        char first = token.charAt(sig);
        String tampered = token.substring(0, sig) + (first == 'A' ? 'B' : 'A') + token.substring(sig + 1);

        assertThatThrownBy(() -> jwtService.extractSubject(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService("a-totally-different-secret-32-bytes-long!!");
        String foreignToken = other.issueToken("subject").token();

        assertThatThrownBy(() -> jwtService.extractSubject(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void failsFastOnMissingSecret() {
        assertThatThrownBy(() -> new JwtService(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void failsFastOnShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
