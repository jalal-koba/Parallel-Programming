package com.example.ecosystem.service;

import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.dto.UserRequest;
import com.example.ecosystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(request.role() == null ? Role.customer : request.role());
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    public User updateUser(Long userId, UserRequest request) {
        User user = getUser(userId);
        userRepository.findByUsername(request.username())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Username already exists: " + request.username());
                });
        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Email already exists: " + request.email());
                });

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(request.role() == null ? Role.customer : request.role());
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = getUser(userId);
        userRepository.delete(user);
    }
}
