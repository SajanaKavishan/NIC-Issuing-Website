package com.project.nic.service;

import com.project.nic.model.User;
import com.project.nic.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    public static final Set<String> ALLOWED_ROLES = Set.of(
            "CITIZEN", "ADMIN", "FINANCE", "DELIVERY", "PRO", "RECOVERY", "ASSISTANT"
    );

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    public User saveUser(User user) {
        normalizeUser(user);
        if (user.getPassword() != null && !user.getPassword().isBlank() && !isBcryptHash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(normalizeEmail(email));
        if (userOpt.isEmpty() || rawPassword == null) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            return Optional.empty();
        }

        if (isBcryptHash(storedPassword) && passwordEncoder.matches(rawPassword, storedPassword)) {
            return Optional.of(user);
        }

        if (!isBcryptHash(storedPassword) && storedPassword.equals(rawPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return Optional.of(user);
        }

        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public String normalizeRole(String role) {
        return role == null ? null : role.trim().toUpperCase();
    }

    public boolean isAllowedRole(String role) {
        return ALLOWED_ROLES.contains(normalizeRole(role));
    }

    private void normalizeUser(User user) {
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("CITIZEN");
        } else {
            user.setRole(normalizeRole(user.getRole()));
        }
        if (!isAllowedRole(user.getRole())) {
            throw new IllegalArgumentException("Invalid role: " + user.getRole());
        }
        user.setEmail(normalizeEmail(user.getEmail()));
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
