package com.example.rbpolab1.service;

import com.example.rbpolab1.model.User;
import com.example.rbpolab1.repository.ShipmentRepository;
import com.example.rbpolab1.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class UserService {

    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;

    public UserService(UserRepository userRepository,
                       ShipmentRepository shipmentRepository) {
        this.userRepository = userRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));
    }

    public User create(String name) {
        String validName = validateName(name);
        User user = new User(null, validName);

        return userRepository.save(user);
    }

    public User update(Long id, String name) {
        User existingUser = findById(id);

        if (name == null) {
            return existingUser;
        }

        String validName = validateName(name);
        existingUser.setName(validName);

        return userRepository.save(existingUser);
    }

    public void delete(Long id) {
        User existingUser = findById(id);

        if (shipmentRepository.existsBySenderId(id)) {
            throw new IllegalStateException(
                    "Cannot delete a user with shipments"
            );
        }

        userRepository.delete(existingUser);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        String validName = name.trim();

        if (validName.length() > 255) {
            throw new IllegalArgumentException(
                    "Name must not exceed 255 characters"
            );
        }

        return validName;
    }
}