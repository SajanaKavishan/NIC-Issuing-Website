package com.project.nic.config;

import com.project.nic.model.User;
import com.project.nic.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StaffAccountInitializer implements CommandLineRunner {
    private final UserService userService;

    public StaffAccountInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        Map<String, String> staffRoles = Map.of(
                "admin@gmail.com", "ADMIN",
                "finance@gmail.com", "FINANCE",
                "delivery@gmail.com", "DELIVERY",
                "pro@gmail.com", "PRO",
                "recovery@gmail.com", "RECOVERY",
                "assistant@gmail.com", "ASSISTANT"
        );

        staffRoles.forEach((email, role) -> {
            userService.findByEmail(email).ifPresentOrElse(existing -> {
                if (existing.getRole() == null || !role.equalsIgnoreCase(existing.getRole())) {
                    existing.setRole(role);
                    userService.saveUser(existing);
                }
            }, () -> {
                User user = new User();
                user.setFirstName(role.substring(0, 1) + role.substring(1).toLowerCase());
                user.setLastName("User");
                user.setEmail(email);
                user.setPassword("1234");
                user.setRole(role);
                userService.saveUser(user);
            });
        });
    }
}
