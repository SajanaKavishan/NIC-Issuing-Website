package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.LostNicDto;
import com.project.nic.dto.ApiDtos.LostNicUpdateRequest;
import com.project.nic.dto.ApiDtos.StatusUpdateRequest;
import com.project.nic.model.LostNic;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.LostNicService;
import com.project.nic.util.FileUploadUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

// New imports for file serving
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/lost-nic")
@Validated
public class LostNicController {
    private static final Logger logger = LoggerFactory.getLogger(LostNicController.class);

    @Autowired
    private LostNicService service;

    @Autowired
    private AuthAccessService authAccessService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostMapping("/submit")
    public ResponseEntity<String> submitLostNic(
        @NotBlank @RequestParam String nicNumber,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") @RequestParam String lostDate,
        @Pattern(regexp = "^[0-9+\\-()\\s]{7,20}$") @RequestParam String contactNumber,
        @RequestParam("birthCertificate") MultipartFile birthCertificate,
        @RequestParam("policeReport") MultipartFile policeReport,
        @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) throws IOException {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }

        String birthCertPath;
        String policeReportPath;
        try {
            FileUploadUtil.validateDocument(birthCertificate);
            FileUploadUtil.validateDocument(policeReport);
            birthCertPath = FileUploadUtil.saveDocument(uploadDir, birthCertificate);
            policeReportPath = FileUploadUtil.saveDocument(uploadDir, policeReport);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        LostNic lostNic = new LostNic();
        lostNic.setNicNumber(nicNumber);
        lostNic.setLostDate(LocalDate.parse(lostDate));
        lostNic.setContactNumber(contactNumber);
        lostNic.setBirthCertificatePath(birthCertPath);
        lostNic.setPoliceReportPath(policeReportPath);
        lostNic.setUserId(sessionUser.get().userId());
        lostNic.setUserEmail(sessionUser.get().email());

        service.save(lostNic);
        return ResponseEntity.ok("Lost NIC report submitted successfully.");
    }

    // New endpoints for admin operations
    @GetMapping("/all")
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        return ResponseEntity.ok(service.findAll().stream().map(LostNicDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyApplications(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(service.findByUserId(sessionUser.get().userId()).stream().map(LostNicDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        return service.findById(id)
                .<ResponseEntity<?>>map(lostNic -> ResponseEntity.ok(LostNicDto.from(lostNic)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody LostNicUpdateRequest updates,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        logger.info("Received update request for ID: {}", id);
        try {
            return ResponseEntity.ok(LostNicDto.from(service.update(id, updates.toEntity())));
        } catch (Exception e) {
            logger.error("Error updating LostNic with ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        logger.info("Received delete request for ID: {}", id);
        try {
            service.deleteById(id);
            logger.info("Successfully deleted LostNic with ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("LostNic with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting LostNic with ID: {}", id, e);
            return ResponseEntity.status(500).body("Failed to delete request");
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getAllLostNicRequests(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        return ResponseEntity.ok(service.findAll().stream().map(LostNicDto::from).collect(Collectors.toList()));
    }

    // New endpoint to update only the status of a LostNic request
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest payload,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        logger.info("Received status update for ID: {} with payload: {}", id, payload);
        try {
            LostNic updated = service.updateStatus(id, payload.status);
            return ResponseEntity.ok(LostNicDto.from(updated));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status update or not found: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to update status for ID: {}", id, e);
            return ResponseEntity.status(500).body("Failed to update status");
        }
    }

    // New endpoint to serve uploaded files (birthCertificate or policeReport)
    @GetMapping("/{id}/file")
    public ResponseEntity<?> serveFile(
            @PathVariable Long id,
            @RequestParam String type,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageLostNic(token)) {
            return ResponseEntity.status(403).body("Recovery access required");
        }
        try {
            LostNic lostNic = service.findById(id).orElseThrow(() -> new IllegalArgumentException("LostNic with id " + id + " not found"));

            String storedPath;
            if ("birthCertificate".equalsIgnoreCase(type)) storedPath = lostNic.getBirthCertificatePath();
            else if ("policeReport".equalsIgnoreCase(type)) storedPath = lostNic.getPoliceReportPath();
            else return ResponseEntity.badRequest().body("Invalid file type");

            if (storedPath == null || storedPath.isBlank()) {
                return ResponseEntity.notFound().build();
            }

            // Securely resolve file inside the configured private upload directory.
            Path uploadsDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            String fileName = Paths.get(storedPath).getFileName().toString();
            Path filePath = uploadsDir.resolve(fileName).normalize();

            if (!filePath.startsWith(uploadsDir) || !Files.exists(filePath)) {
                logger.warn("Requested file not found or outside upload directory: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error serving file for id {} type {}", id, type, e);
            return ResponseEntity.status(500).body("Failed to read file");
        }
    }
}
