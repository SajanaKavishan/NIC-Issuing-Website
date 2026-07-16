package com.project.nic.controller;

import com.project.nic.model.User;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthSessionService authSessionService;

    @PostMapping("/signup")
    public String signup(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return "Email already registered";
        }
        user.setRole("CITIZEN");
        userService.saveUser(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody User loginUser) {
        Optional<User> userOpt = userService.authenticate(loginUser.getEmail(), loginUser.getPassword());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(LoginResponse.failure("Invalid credentials"));
        }

        User user = userOpt.get();
        String token = authSessionService.createSession(user);
        return ResponseEntity.ok(LoginResponse.success(token, UserDto.from(user)));
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authSessionService.hasRole(token, "ADMIN")) {
            return ResponseEntity.status(403).body("Admin access required");
        }
        return ResponseEntity.ok(userService.findAll().stream().map(UserDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/by-email")
    public UserDto getByEmail(@RequestParam("email") String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        return userOpt.map(UserDto::from).orElse(null);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable Long id,
            @RequestBody User payload,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authSessionService.hasRole(token, "ADMIN")) {
            return ResponseEntity.status(403).body("Admin access required");
        }
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        User u = existing.get();
        if (payload.getFirstName() != null) u.setFirstName(payload.getFirstName());
        if (payload.getLastName() != null) u.setLastName(payload.getLastName());
        if (payload.getEmail() != null) {
            // If email is changed, ensure uniqueness
            if (!payload.getEmail().equalsIgnoreCase(u.getEmail()) && userService.emailExists(payload.getEmail())) {
                return ResponseEntity.badRequest().body("Email already registered");
            }
            u.setEmail(payload.getEmail());
        }
        if (payload.getPassword() != null && !payload.getPassword().isEmpty()) u.setPassword(payload.getPassword());
        userService.saveUser(u);
        return ResponseEntity.ok("User updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authSessionService.hasRole(token, "ADMIN")) {
            return ResponseEntity.status(403).body("Admin access required");
        }
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        userService.deleteById(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    public static class UserDto {
        public Long id;
        public String firstName;
        public String lastName;
        public String email;
        public String role;

        public static UserDto from(User u) {
            UserDto d = new UserDto();
            d.id = u.getId();
            d.firstName = u.getFirstName();
            d.lastName = u.getLastName();
            d.email = u.getEmail();
            d.role = u.getRole() == null || u.getRole().isBlank() ? "CITIZEN" : u.getRole();
            return d;
        }
    }

    public static class LoginResponse {
        public boolean success;
        public String message;
        public String token;
        public UserDto user;

        public static LoginResponse success(String token, UserDto user) {
            LoginResponse response = new LoginResponse();
            response.success = true;
            response.message = "Login successful";
            response.token = token;
            response.user = user;
            return response;
        }

        public static LoginResponse failure(String message) {
            LoginResponse response = new LoginResponse();
            response.success = false;
            response.message = message;
            return response;
        }
    }
}
