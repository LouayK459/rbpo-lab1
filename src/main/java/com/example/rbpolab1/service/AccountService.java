package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Account;
import com.example.rbpolab1.model.AccountRole;
import com.example.rbpolab1.model.AccountStatus;
import com.example.rbpolab1.model.User;
import com.example.rbpolab1.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, UserService userService,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountView register(String name, String username, String password) {
        return toView(createAccount(name, username, password, AccountRole.SENDER));
    }

    public AccountView currentAccount(String username) {
        Account account = accountRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new NoSuchElementException("Account not found"));
        return toView(account);
    }

    public List<AccountView> findAll() {
        return accountRepository.findAll().stream()
                .map(AccountService::toView)
                .toList();
    }

    public AccountView changeStatus(Long id, AccountStatus status, String actorUsername) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Account id must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("Account status is required");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));

        if (status == AccountStatus.BLOCKED && account.getUsername().equals(actorUsername)) {
            throw new IllegalStateException("Cannot block your own account");
        }

        account.setStatus(status);
        return toView(account);
    }

    public void bootstrapDispatcher(String name, String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        Account existing = accountRepository.findByUsername(normalizedUsername).orElse(null);

        if (existing != null) {
            if (existing.getRole() != AccountRole.DISPATCHER) {
                throw new IllegalStateException("Bootstrap username is already in use");
            }
            return;
        }

        createAccount(name, normalizedUsername, password, AccountRole.DISPATCHER);
    }

    private Account createAccount(String name, String username, String password, AccountRole role) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);

        if (accountRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("Username is already in use");
        }

        User user = userService.create(name);
        String passwordHash = passwordEncoder.encode(password);
        return accountRepository.save(new Account(user, normalizedUsername, passwordHash, role));
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

        if (!normalized.matches("[a-z0-9][a-z0-9._-]{2,63}")) {
            throw new IllegalArgumentException(
                    "Username must contain 3-64 characters: letters, digits, dot, underscore or hyphen; "
                            + "the first character must be a letter or digit");
        }

        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must have at least 12 characters and at most 72 UTF-8 bytes");
        }

        boolean upper = password.codePoints().anyMatch(Character::isUpperCase);
        boolean lower = password.codePoints().anyMatch(Character::isLowerCase);
        boolean digit = password.codePoints().anyMatch(Character::isDigit);
        boolean special = password.codePoints().anyMatch(character ->
                !Character.isLetterOrDigit(character) && !Character.isWhitespace(character)
                        && !Character.isISOControl(character));
        boolean control = password.codePoints().anyMatch(Character::isISOControl);

        if (!upper || !lower || !digit || !special || control) {
            throw new IllegalArgumentException(
                    "Password must include uppercase and lowercase letters, a digit and a special character; "
                            + "control characters are not allowed");
        }
    }

    private static AccountView toView(Account account) {
        return new AccountView(account.getId(), account.getUserId(), account.getUsername(),
                account.getRole(), account.getStatus());
    }

    public record AccountView(Long id, Long userId, String username,
                              AccountRole role, AccountStatus status) {
    }
}