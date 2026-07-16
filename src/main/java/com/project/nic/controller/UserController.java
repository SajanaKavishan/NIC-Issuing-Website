package com.project.nic.controller;

import com.project.nic.model.User;
import com.project.nic.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public String signup(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return "Email already registered";
        }
        userService.saveUser(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody User loginUser) {
        Optional<User> userOpt = userService.findByEmail(loginUser.getEmail());
        return userOpt.filter(u -> u.getPassword().equals(loginUser.getPassword()))
                .map(u -> "Login successful")
                .orElse("Invalid credentials");
    }

    @GetMapping
    public List<UserDto> getAll() {
        return userService.findAll().stream().map(UserDto::from).collect(Collectors.toList());
    }

    @GetMapping("/by-email")
    public UserDto getByEmail(@RequestParam("email") String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        return userOpt.map(UserDto::from).orElse(null);
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @RequestBody User payload) {
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return "User not found";
        }
        User u = existing.get();
        if (payload.getFirstName() != null) u.setFirstName(payload.getFirstName());
        if (payload.getLastName() != null) u.setLastName(payload.getLastName());
        if (payload.getEmail() != null) {
            // If email is changed, ensure uniqueness
            if (!payload.getEmail().equalsIgnoreCase(u.getEmail()) && userService.emailExists(payload.getEmail())) {
                return "Email already registered";
            }
            u.setEmail(payload.getEmail());
        }
        if (payload.getPassword() != null && !payload.getPassword().isEmpty()) u.setPassword(payload.getPassword());
        userService.saveUser(u);
        return "User updated successfully";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return "User not found";
        }
        userService.deleteById(id);
        return "User deleted successfully";
    }

    public static class UserDto {
        public Long id;
        public String firstName;
        public String lastName;
        public String email;

        public static UserDto from(User u) {
            UserDto d = new UserDto();
            d.id = u.getId();
            d.firstName = u.getFirstName();
            d.lastName = u.getLastName();
            d.email = u.getEmail();
            return d;
        }
    }
}
