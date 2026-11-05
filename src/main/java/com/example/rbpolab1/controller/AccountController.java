package com.example.rbpolab1.controller;

import com.example.rbpolab1.model.AccountStatus;
import com.example.rbpolab1.service.AccountService;
import com.example.rbpolab1.service.AccountService.AccountView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('DISPATCHER')")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountView> findAll() {
        return accountService.findAll();
    }

    @PatchMapping("/{id}/status")
    public AccountView changeStatus(@PathVariable Long id,
                                    @RequestBody StatusRequest request,
                                    Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        return accountService.changeStatus(id, request.status(), authentication.getName());
    }

    public record StatusRequest(AccountStatus status) {
    }
}