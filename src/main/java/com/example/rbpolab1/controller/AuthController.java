package com.example.rbpolab1.controller;

import com.example.rbpolab1.service.AccountService;
import com.example.rbpolab1.service.AuthenticationService;
import com.example.rbpolab1.service.TokenPairService;
import com.example.rbpolab1.service.TokenPairService.TokenPair;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AccountService accounts;
    private final AuthenticationService authentication;
    private final TokenPairService tokens;

    public AuthController(AccountService accounts, AuthenticationService authentication, TokenPairService tokens) {
        this.accounts = accounts;
        this.authentication = authentication;
        this.tokens = tokens;
    }

    @PostMapping("/auth/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> login(@RequestBody LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        return response(HttpStatus.OK,
                authentication.login(request.username(), request.password(), request.deviceId()));
    }

    @PostMapping("/auth/register")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> register(@RequestBody RegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        return response(HttpStatus.CREATED, authentication.register(
                request.name(), request.username(), request.password(), request.deviceId()));
    }

    @PostMapping("/auth/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        // The transaction commits before a replay refusal is translated into HTTP 401.
        TokenPair pair = tokens.refresh(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh session"));
        return response(HttpStatus.OK, pair);
    }

    @GetMapping("/api/auth/me")
    @PreAuthorize("isAuthenticated()")
    public AccountService.AccountView me(Authentication authentication) {
        return accounts.currentAccount(authentication.getName());
    }

    private ResponseEntity<TokenPair> response(HttpStatus status, TokenPair pair) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(pair);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(String username, String password, String deviceId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistrationRequest(String name, String username, String password, String deviceId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefreshRequest(String refreshToken) {
    }
}
