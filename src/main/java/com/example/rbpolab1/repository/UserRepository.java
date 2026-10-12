package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public synchronized Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public synchronized boolean existsById(Long id) {
        return users.containsKey(id);
    }

    public synchronized User save(User user) {
        if (user.getId() == null) {
            user.setId(nextId++);
        }

        users.put(user.getId(), user);
        return user;
    }

    public synchronized void deleteById(Long id) {
        users.remove(id);
    }
}