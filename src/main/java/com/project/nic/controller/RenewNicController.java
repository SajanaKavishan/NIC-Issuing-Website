package com.project.nic.controller;

import com.project.nic.model.RenewNic;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.RenewNicService;
import com.project.nic.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/renew-nic")
public class RenewNicController {
    @Autowired
    private RenewNicService service;

    @Autowired
    private AuthSessionService authSessionService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private boolean canManageApplications(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "PRO", "RECOVERY");
    }

    private Optional<AuthSessionService.SessionUser> getLoggedInUser(String token) {
        return authSessionService.findByToken(token);
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitRenewNic(
        @RequestParam String oldNicNumber,
        @RequestParam String birthdate,
        @RequestParam String reason,
        @RequestParam(required = false) String otherReason,
        @RequestParam String contactNumber,
        @RequestParam("birthCertificate") MultipartFile birthCertificate,
        @RequestParam("photo") MultipartFile photo,
        @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) throws IOException {
        Optional<AuthSessionService.SessionUser> sessionUser = getLoggedInUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }

        String birthCertPath;
        String photoPath;
        try {
            FileUploadUtil.validateDocument(birthCertificate);
            FileUploadUtil.validateImage(photo);
            birthCertPath = FileUploadUtil.saveDocument(uploadDir, birthCertificate);
            photoPath = FileUploadUtil.saveImage(uploadDir, photo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        RenewNic renewNic = new RenewNic();
        renewNic.setOldNicNumber(oldNicNumber);
        renewNic.setBirthdate(LocalDate.parse(birthdate));
        renewNic.setReason(reason);
        renewNic.setOtherReason(otherReason);
        renewNic.setContactNumber(contactNumber);
        renewNic.setBirthCertificatePath(birthCertPath);
        renewNic.setPhotoPath(photoPath);
        renewNic.setUserId(sessionUser.get().userId());
        renewNic.setUserEmail(sessionUser.get().email());

        service.save(renewNic);
        return ResponseEntity.ok("NIC renewal request submitted successfully.");
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRenewNics(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyApplications(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = getLoggedInUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(service.findByUserId(sessionUser.get().userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        return service.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        try {
            return ResponseEntity.ok(service.updateStatus(id, payload.get("status")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
