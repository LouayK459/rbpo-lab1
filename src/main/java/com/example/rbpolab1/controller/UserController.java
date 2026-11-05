package com.example.rbpolab1.controller;

import com.example.rbpolab1.model.User;
import com.example.rbpolab1.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DISPATCHER')")
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessRules.canAccessUser(#id)")
    public User findById(@PathVariable("id") Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<User> create(@RequestBody User request) {
        validateRequest(request);
        User user = userService.create(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@accessRules.canAccessUser(#id)")
    public User update(
            @PathVariable("id") Long id,
            @RequestBody User request) {
        validateRequest(request);
        return userService.update(id, request.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessRules.canAccessUser(#id)")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateRequest(User request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
    }
}