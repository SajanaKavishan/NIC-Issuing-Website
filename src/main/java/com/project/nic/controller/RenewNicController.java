package com.project.nic.controller;

import com.project.nic.model.RenewNic;
import com.project.nic.service.RenewNicService;
import com.project.nic.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/renew-nic")
public class RenewNicController {
    @Autowired
    private RenewNicService service;

    @PostMapping("/submit")
    public String submitRenewNic(
        @RequestParam String oldNicNumber,
        @RequestParam String birthdate,
        @RequestParam String reason,
        @RequestParam(required = false) String otherReason,
        @RequestParam String contactNumber,
        @RequestParam("birthCertificate") MultipartFile birthCertificate,
        @RequestParam("photo") MultipartFile photo
    ) throws IOException {
        String birthCertPath = FileUploadUtil.saveFile("uploads", birthCertificate);
        String photoPath = FileUploadUtil.saveFile("uploads", photo);

        RenewNic renewNic = new RenewNic();
        renewNic.setOldNicNumber(oldNicNumber);
        renewNic.setBirthdate(LocalDate.parse(birthdate));
        renewNic.setReason(reason);
        renewNic.setOtherReason(otherReason);
        renewNic.setContactNumber(contactNumber);
        renewNic.setBirthCertificatePath(birthCertPath);
        renewNic.setPhotoPath(photoPath);

        service.save(renewNic);
        return "NIC renewal request submitted successfully.";
    }

    @GetMapping("/all")
    public List<RenewNic> getAllRenewNics() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return service.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            return ResponseEntity.ok(service.updateStatus(id, payload.get("status")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
