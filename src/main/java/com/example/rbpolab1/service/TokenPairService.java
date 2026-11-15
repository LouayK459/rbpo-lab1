package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Account;
import com.example.rbpolab1.model.AccountStatus;
import com.example.rbpolab1.model.SessionStatus;
import com.example.rbpolab1.model.UserSession;
import com.example.rbpolab1.repository.AccountRepository;
import com.example.rbpolab1.repository.UserSessionRepository;
import com.example.rbpolab1.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class TokenPairService {
    private final UserSessionRepository sessions;
    private final AccountRepository accounts;
    private final JwtTokenProvider jwt;
    private final Clock clock;

    public TokenPairService(UserSessionRepository sessions, AccountRepository accounts,
                            JwtTokenProvider jwt, Clock clock) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TokenPair issue(String username, String deviceId) {
        Account account = activeAccount(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return savePair(account.getUsername(), normalizeDeviceId(deviceId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<TokenPair> refresh(String refreshToken) {
        Claims claims = jwt.validateRefresh(refreshToken);
        // PostgreSQL holds the row lock until commit; a waiting request reads the new status.
        UserSession old = sessions.findForRefresh(refreshToken).orElse(null);
        Instant now = clock.instant();
        if (old == null || !old.getUsername().equals(claims.getSubject())
                || !old.getRefreshTokenExpiry().isAfter(now)
                || !old.getRefreshTokenExpiry().equals(claims.getExpiration().toInstant())) {
            return Optional.empty();
        }
        if (old.getStatus() == SessionStatus.USED) {
            old.revoke();
            sessions.flush();
            // Return normally so REVOKED commits. The controller sends 401 after the transaction.
            return Optional.empty();
        }
        if (old.getStatus() != SessionStatus.ACTIVE || activeAccount(old.getUsername()).isEmpty()) {
            return Optional.empty();
        }
        old.markUsed();
        // Any persistence failure rolls back both this status change and the new row.
        return Optional.of(savePair(old.getUsername(), old.getDeviceId()));
    }

    private TokenPair savePair(String username, String deviceId) {
        JwtTokenProvider.IssuedToken access = jwt.createAccess(username);
        JwtTokenProvider.IssuedToken refresh = jwt.createRefresh(username);
        sessions.saveAndFlush(new UserSession(username, deviceId, access.value(), refresh.value(),
                access.expiresAt(), refresh.expiresAt()));
        return new TokenPair(access.value(), refresh.value());
    }

    private Optional<Account> activeAccount(String username) {
        return accounts.findByUsername(username)
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE);
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) { return null; }
        if (deviceId.length() > 255) {
            throw new IllegalArgumentException("Device id must not exceed 255 characters");
        }
        return deviceId.trim();
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }
}
