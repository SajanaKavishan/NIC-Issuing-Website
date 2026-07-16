package com.project.nic.controller;

import com.project.nic.model.LostNic;
import com.project.nic.service.LostNicService;
import com.project.nic.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lost-nic")
public class LostNicController {
    private static final Logger logger = LoggerFactory.getLogger(LostNicController.class);

    @Autowired
    private LostNicService service;

    @PostMapping("/submit")
    public String submitLostNic(
        @RequestParam String nicNumber,
        @RequestParam String lostDate,
        @RequestParam String contactNumber,
        @RequestParam("birthCertificate") MultipartFile birthCertificate,
        @RequestParam("policeReport") MultipartFile policeReport
    ) throws IOException {
        String birthCertPath = FileUploadUtil.saveFile("uploads", birthCertificate);
        String policeReportPath = FileUploadUtil.saveFile("uploads", policeReport);

        LostNic lostNic = new LostNic();
        lostNic.setNicNumber(nicNumber);
        lostNic.setLostDate(LocalDate.parse(lostDate));
        lostNic.setContactNumber(contactNumber);
        lostNic.setBirthCertificatePath(birthCertPath);
        lostNic.setPoliceReportPath(policeReportPath);

        service.save(lostNic);
        return "Lost NIC report submitted successfully.";
    }

    // New endpoints for admin operations
    @GetMapping("/all")
    public List<LostNic> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public LostNic getById(@PathVariable Long id) {
        return service.findById(id).orElseThrow(() -> new IllegalArgumentException("LostNic with id " + id + " not found"));
    }

    @PutMapping("/{id}")
    public LostNic update(@PathVariable Long id, @RequestBody LostNic updates) {
        logger.info("Received update request for ID: {} with data: {}", id, updates);
        try {
            return service.update(id, updates);
        } catch (Exception e) {
            logger.error("Error updating LostNic with ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
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
    public List<LostNic> getAllLostNicRequests() {
        return service.findAll();
    }

    // New endpoint to update only the status of a LostNic request
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        logger.info("Received status update for ID: {} with payload: {}", id, payload);
        try {
            String status = payload.get("status");
            LostNic updated = service.updateStatus(id, status);
            return ResponseEntity.ok(updated);
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
    public ResponseEntity<?> serveFile(@PathVariable Long id, @RequestParam String type) {
        try {
            LostNic lostNic = service.findById(id).orElseThrow(() -> new IllegalArgumentException("LostNic with id " + id + " not found"));

            String storedPath;
            if ("birthCertificate".equalsIgnoreCase(type)) storedPath = lostNic.getBirthCertificatePath();
            else if ("policeReport".equalsIgnoreCase(type)) storedPath = lostNic.getPoliceReportPath();
            else return ResponseEntity.badRequest().body("Invalid file type");

            if (storedPath == null || storedPath.isBlank()) {
                return ResponseEntity.notFound().build();
            }

            // Securely resolve file inside the uploads directory
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            String fileName = Paths.get(storedPath).getFileName().toString();
            Path filePath = uploadsDir.resolve(fileName).normalize();

            if (!filePath.startsWith(uploadsDir) || !Files.exists(filePath)) {
                logger.warn("Requested file not found or outside uploads: {}", filePath);
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
