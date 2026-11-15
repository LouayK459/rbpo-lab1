package com.example.rbpolab1.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final AccountService accounts;
    private final TokenPairService tokens;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 AccountService accounts, TokenPairService tokens) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
        this.tokens = tokens;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TokenPairService.TokenPair login(String username, String password, String deviceId) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        Authentication authenticated = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        return tokens.issue(authenticated.getName(), deviceId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TokenPairService.TokenPair register(String name, String username, String password, String deviceId) {
        // Retain AccountService validation, uniqueness, BCrypt and the fixed SENDER role.
        AccountService.AccountView account = accounts.register(name, username, password);
        return tokens.issue(account.username(), deviceId);
    }
}
