package com.example.rbpolab1.controller;

import com.example.rbpolab1.service.AccountService;
import com.example.rbpolab1.service.AccountService.AccountView;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/csrf")
    @PreAuthorize("permitAll()")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AccountView> register(@RequestBody RegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        AccountView account = accountService.register(
                request.name(), request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AccountView me(Authentication authentication) {
        return accountService.currentAccount(authentication.getName());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistrationRequest(String name, String username, String password) {
    }
}