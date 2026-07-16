package com.project.nic.controller;

import com.project.nic.model.NewNicForm;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.NewNicFormService;
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
@RequestMapping("/api/new-nic")
public class NewNicFormController {

    @Autowired
    private NewNicFormService service;

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManageApplications(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "PRO", "RECOVERY");
    }

    @PostMapping("/submit")
    public String submitForm(
            @RequestParam String nameWithInitials,
            @RequestParam String gender,
            @RequestParam int age,
            @RequestParam String civilStatus,
            @RequestParam String profession,
            @RequestParam String birthdate,
            @RequestParam String address,
            @RequestParam String contactNumber,
            @RequestParam("birthCertificate") MultipartFile birthCertificate,
            @RequestParam("photo") MultipartFile photo
    ) throws IOException {

        String birthCertPath = FileUploadUtil.saveFile("uploads", birthCertificate);
        String photoPath = FileUploadUtil.saveFile("uploads", photo);

        NewNicForm form = new NewNicForm();
        form.setNameWithInitials(nameWithInitials);
        form.setGender(gender);
        form.setAge(age);
        form.setCivilStatus(civilStatus);
        form.setProfession(profession);
        form.setBirthdate(LocalDate.parse(birthdate));
        form.setAddress(address);
        form.setContactNumber(contactNumber);
        form.setBirthCertificatePath(birthCertPath);
        form.setPhotoPath(photoPath);

        service.save(form);
        return "New NIC application submitted successfully.";
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        return ResponseEntity.ok(service.findAll());
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
