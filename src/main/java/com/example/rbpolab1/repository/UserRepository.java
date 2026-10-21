package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}