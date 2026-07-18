package com.project.nic.service;

import com.project.nic.model.AssistanceRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthAccessService {
    private final AuthSessionService authSessionService;

    public AuthAccessService(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    public Optional<AuthSessionService.SessionUser> currentUser(String token) {
        return authSessionService.findByToken(token);
    }

    public boolean isAdmin(String token) {
        return authSessionService.hasRole(token, "ADMIN");
    }

    public boolean canManageApplications(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "PRO", "RECOVERY");
    }

    public boolean canManageAssistance(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "ASSISTANT");
    }

    public boolean canManageDelivery(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "DELIVERY");
    }

    public boolean canManageFeedback(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "PRO");
    }

    public boolean canManageLostNic(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "RECOVERY");
    }

    public boolean canManagePayments(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "FINANCE");
    }

    public boolean ownsAssistanceRequest(AuthSessionService.SessionUser sessionUser, AssistanceRequest request) {
        if (request.getUserId() != null && request.getUserId().equals(sessionUser.userId())) {
            return true;
        }
        return request.getEmail() != null && request.getEmail().equalsIgnoreCase(sessionUser.email());
    }
}
