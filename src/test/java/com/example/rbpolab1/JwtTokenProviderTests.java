package com.example.rbpolab1;

import com.example.rbpolab1.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTests {
    private final Instant now = Instant.parse("2030-01-01T12:00:00.987654Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final SecretKey key = Jwts.SIG.HS256.key().build();
    private final String secret = Encoders.BASE64.encode(key.getEncoded());
    private final JwtTokenProvider jwt = new JwtTokenProvider(secret, 900000, 604800000, clock);

    @Test
    void tokensHaveUniqueIdsCorrectTypesAndExactSecondPrecisionExpiry() {
        var first = jwt.createAccess("sender");
        var second = jwt.createAccess("sender");
        var refresh = jwt.createRefresh("sender");
        Claims claims = jwt.validateAccess(first.value());
        assertEquals("sender", claims.getSubject());
        assertEquals("access", claims.get("type"));
        assertNotEquals(first.value(), second.value());
        assertNotEquals(claims.getId(), jwt.validateAccess(second.value()).getId());
        assertNotEquals(claims.getId(), jwt.validateRefresh(refresh.value()).getId());
        assertEquals(first.expiresAt(), claims.getExpiration().toInstant());
        assertEquals(0, first.expiresAt().getNano());
        assertEquals(900, first.expiresAt().getEpochSecond() - claims.getIssuedAt().toInstant().getEpochSecond());
        assertEquals(604800, refresh.expiresAt().getEpochSecond() - claims.getIssuedAt().toInstant().getEpochSecond());
    }

    @Test
    void accessAndRefreshCannotBeInterchanged() {
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(jwt.createRefresh("sender").value()));
        assertThrows(BadCredentialsException.class, () -> jwt.validateRefresh(jwt.createAccess("sender").value()));
    }

    @Test
    void malformedBadSignatureAndExpiredTokensAreRejected() {
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess("not.a.jwt"));
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(null));
        String wrongKey = Jwts.builder().claims(validClaims()).signWith(Jwts.SIG.HS256.key().build()).compact();
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(wrongKey));
        var expired = new JwtTokenProvider(secret, 900000, 604800000,
                Clock.fixed(now.minusSeconds(901), ZoneOffset.UTC)).createAccess("sender");
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(expired.value()));
        var atExpiry = new JwtTokenProvider(secret, 900000, 604800000,
                Clock.fixed(jwt.createAccess("sender").expiresAt(), ZoneOffset.UTC));
        assertThrows(BadCredentialsException.class, () -> atExpiry.validateAccess(jwt.createAccess("sender").value()));
    }

    @Test
    void everyRequiredClaimAndItsValueAreCheckedEvenWithAValidSignature() {
        for (String required : new String[]{"sub", "jti", "iat", "exp", "type"}) {
            Map<String, Object> claims = validClaims();
            claims.remove(required);
            String token = signed(claims);
            assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(token), required);
        }
        for (String required : new String[]{"sub", "jti", "type"}) {
            Map<String, Object> claims = validClaims();
            claims.put(required, " ");
            assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(signed(claims)), required);
        }
        Map<String, Object> future = validClaims();
        future.put("iat", Date.from(now.plusSeconds(30)));
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(signed(future)));
        Map<String, Object> wrongType = validClaims();
        wrongType.put("type", 123);
        assertThrows(BadCredentialsException.class, () -> jwt.validateAccess(signed(wrongType)));
    }

    @Test
    void otherSigningAlgorithmsAndInvalidLifetimesAreRejected() {
        SecretKey longKey = Jwts.SIG.HS512.key().build();
        var provider = new JwtTokenProvider(Encoders.BASE64.encode(longKey.getEncoded()), 900000, 604800000, clock);
        String hs512 = Jwts.builder().claims(validClaims()).signWith(longKey, Jwts.SIG.HS512).compact();
        assertThrows(BadCredentialsException.class, () -> provider.validateAccess(hs512));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(secret, 999, 1000, clock));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(secret, 1000, 1001, clock));
        assertThrows(RuntimeException.class, () -> new JwtTokenProvider(
                Encoders.BASE64.encode(new byte[16]), 1000, 1000, clock));
    }

    private Map<String, Object> validClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "sender");
        claims.put("jti", "test-token");
        claims.put("iat", Date.from(now.minusSeconds(1)));
        claims.put("exp", Date.from(now.plusSeconds(60)));
        claims.put("type", "access");
        return claims;
    }

    private String signed(Map<String, Object> claims) {
        return Jwts.builder().claims(claims).signWith(key, Jwts.SIG.HS256).compact();
    }
}
