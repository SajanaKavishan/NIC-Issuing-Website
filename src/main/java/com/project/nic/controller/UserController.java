package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.UserRequest;
import com.project.nic.model.User;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;
    private final AuthSessionService authSessionService;
    private final AuthAccessService authAccessService;

    public UserController(
            UserService userService,
            AuthSessionService authSessionService,
            AuthAccessService authAccessService
    ) {
        this.userService = userService;
        this.authSessionService = authSessionService;
        this.authAccessService = authAccessService;
    }

    @PostMapping("/signup")
    public String signup(@Valid @RequestBody UserRequest request) {
        User user = request.toEntity();
        if (userService.emailExists(user.getEmail())) {
            return "Email already registered";
        }
        user.setRole("CITIZEN");
        userService.saveUser(user);
        return "User registered successfully";
    }

    @PostMapping
    public ResponseEntity<String> create(
            @Valid @RequestBody UserRequest request,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.isAdmin(token)) {
            return ResponseEntity.status(403).body("Admin access required");
        }
        User payload = request.toEntity();
        if (payload.getRole() == null || payload.getRole().isBlank()) {
            return ResponseEntity.badRequest().body("Role is required");
        }
        if (!userService.isAllowedRole(payload.getRole())) {
            return ResponseEntity.badRequest().body("Invalid role: " + payload.getRole());
        }
        if (payload.getPassword() == null || payload.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Password is required");
        }
        if (userService.emailExists(payload.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        payload.setRole(userService.normalizeRole(payload.getRole()));
        userService.saveUser(payload);
        return ResponseEntity.ok("User created successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody UserRequest loginRequest) {
        Optional<User> userOpt = userService.authenticate(loginRequest.email, loginRequest.password);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(LoginResponse.failure("Invalid credentials"));
        }

        User user = userOpt.get();
        String token = authSessionService.createSession(user);
        return ResponseEntity.ok(LoginResponse.success(token, UserDto.from(user)));
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid or expired session");
        }

        Optional<User> userOpt = userService.findById(sessionUser.get().userId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid or expired session");
        }

        return ResponseEntity.ok(UserDto.from(userOpt.get()));
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.isAdmin(token)) {
            return ResponseEntity.status(403).body("Admin access required");
        }
        return ResponseEntity.ok(userService.findAll().stream().map(UserDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/by-email")
    public UserDto getByEmail(@Email @RequestParam("email") String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        return userOpt.map(UserDto::from).orElse(null);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.isAdmin(token)) {
            return ResponseEntity.status(403).body("Admin access required");
        }
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        User payload = request.toEntity();
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
        if (payload.getRole() != null) {
            if (payload.getRole().isBlank()) {
                return ResponseEntity.badRequest().body("Role is required");
            }
            if (!userService.isAllowedRole(payload.getRole())) {
                return ResponseEntity.badRequest().body("Invalid role: " + payload.getRole());
            }
            u.setRole(userService.normalizeRole(payload.getRole()));
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
        if (!authAccessService.isAdmin(token)) {
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
