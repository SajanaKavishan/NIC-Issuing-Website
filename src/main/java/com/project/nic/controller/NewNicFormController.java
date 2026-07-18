package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.NewNicFormDto;
import com.project.nic.model.NewNicForm;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.NewNicFormService;
import com.project.nic.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/new-nic")
public class NewNicFormController {

    @Autowired
    private NewNicFormService service;

    @Autowired
    private AuthAccessService authAccessService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostMapping("/submit")
    public ResponseEntity<String> submitForm(
            @RequestParam String nameWithInitials,
            @RequestParam String gender,
            @RequestParam int age,
            @RequestParam String civilStatus,
            @RequestParam String profession,
            @RequestParam String birthdate,
            @RequestParam String address,
            @RequestParam String contactNumber,
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
        form.setUserId(sessionUser.get().userId());
        form.setUserEmail(sessionUser.get().email());

        service.save(form);
        return ResponseEntity.ok("New NIC application submitted successfully.");
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        return ResponseEntity.ok(service.findAll().stream().map(NewNicFormDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyApplications(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(service.findByUserId(sessionUser.get().userId()).stream().map(NewNicFormDto::from).collect(Collectors.toList()));
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
                .<ResponseEntity<?>>map(form -> ResponseEntity.ok(NewNicFormDto.from(form)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        try {
            return ResponseEntity.ok(NewNicFormDto.from(service.updateStatus(id, payload.get("status"))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
