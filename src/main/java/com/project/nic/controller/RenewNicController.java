package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.ApplicationSubmissionResponse;
import com.project.nic.dto.ApiDtos.RenewNicDto;
import com.project.nic.dto.ApiDtos.StatusUpdateRequest;
import com.project.nic.model.RenewNic;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.RenewNicService;
import com.project.nic.util.FileUploadUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/renew-nic")
@Validated
public class RenewNicController {
    private final RenewNicService service;
    private final AuthAccessService authAccessService;
    private final String uploadDir;

    public RenewNicController(
            RenewNicService service,
            AuthAccessService authAccessService,
            @Value("${app.upload.dir}") String uploadDir
    ) {
        this.service = service;
        this.authAccessService = authAccessService;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitRenewNic(
        @NotBlank @RequestParam String oldNicNumber,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") @RequestParam String birthdate,
        @NotBlank @RequestParam String reason,
        @RequestParam(required = false) String otherReason,
        @Pattern(regexp = "^[0-9+\\-()\\s]{7,20}$") @RequestParam String contactNumber,
        @RequestParam("birthCertificate") MultipartFile birthCertificate,
        @RequestParam("photo") MultipartFile photo,
        @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) throws IOException {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
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

        RenewNic saved = service.save(renewNic);
        return ResponseEntity.ok(new ApplicationSubmissionResponse("NIC renewal request submitted successfully.", saved.getId()));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRenewNics(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        return ResponseEntity.ok(service.findAll().stream().map(RenewNicDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyApplications(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(service.findByUserId(sessionUser.get().userId()).stream().map(RenewNicDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        return service.findById(id)
                .<ResponseEntity<?>>map(renewNic -> ResponseEntity.ok(RenewNicDto.from(renewNic)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest payload,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        try {
            return ResponseEntity.ok(RenewNicDto.from(service.updateStatus(id, payload.status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
