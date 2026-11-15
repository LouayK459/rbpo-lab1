package com.example.rbpolab1.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final JwtParser parser;
    private final Clock clock;
    private final long accessSeconds;
    private final long refreshSeconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.accessExpiration}") long accessMillis,
                            @Value("${jwt.refreshExpiration}") long refreshMillis,
                            Clock clock) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.clock = clock;
        this.accessSeconds = lifetimeSeconds(accessMillis);
        this.refreshSeconds = lifetimeSeconds(refreshMillis);
        this.parser = Jwts.parser().verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .sig().clear().add(Jwts.SIG.HS256).and().build();
    }

    public IssuedToken createAccess(String username) {
        return create(username, "access", accessSeconds);
    }

    public IssuedToken createRefresh(String username) {
        return create(username, "refresh", refreshSeconds);
    }

    public Claims validateAccess(String token) { return validate(token, "access"); }
    public Claims validateRefresh(String token) { return validate(token, "refresh"); }

    private IssuedToken create(String username, String type, long seconds) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(seconds);
        String value = Jwts.builder().header().type("JWT").and()
                .subject(username).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .claim("type", type).signWith(key, Jwts.SIG.HS256).compact();
        return new IssuedToken(value, expiresAt);
    }

    private Claims validate(String token, String expectedType) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Instant now = clock.instant();
            if (claims.getSubject() == null || claims.getSubject().isBlank()
                    || claims.getId() == null || claims.getId().isBlank()
                    || claims.getIssuedAt() == null || claims.getExpiration() == null
                    || !expectedType.equals(claims.get("type", String.class))
                    || !claims.getExpiration().toInstant().isAfter(now)
                    || claims.getIssuedAt().toInstant().isAfter(now)
                    || !claims.getExpiration().after(claims.getIssuedAt())) {
                throw new BadCredentialsException("Invalid or expired token");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid or expired token");
        }
    }

    private static long lifetimeSeconds(long millis) {
        if (millis < 1000 || millis % 1000 != 0) {
            throw new IllegalArgumentException("JWT lifetimes must be positive whole seconds in milliseconds");
        }
        return millis / 1000;
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
