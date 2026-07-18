package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.ApplicationSubmissionResponse;
import com.project.nic.dto.ApiDtos.NewNicFormDto;
import com.project.nic.dto.ApiDtos.StatusUpdateRequest;
import com.project.nic.model.NewNicForm;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.NewNicFormService;
import com.project.nic.util.FileUploadUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/new-nic")
@Validated
public class NewNicFormController {

    private final NewNicFormService service;
    private final AuthAccessService authAccessService;
    private final String uploadDir;

    public NewNicFormController(
            NewNicFormService service,
            AuthAccessService authAccessService,
            @Value("${app.upload.dir}") String uploadDir
    ) {
        this.service = service;
        this.authAccessService = authAccessService;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitForm(
            @NotBlank @RequestParam String nameWithInitials,
            @NotBlank @RequestParam String gender,
            @Min(1) @Max(120) @RequestParam int age,
            @NotBlank @RequestParam String civilStatus,
            @NotBlank @RequestParam String profession,
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") @RequestParam String birthdate,
            @NotBlank @RequestParam String address,
            @Pattern(regexp = "^[0-9+\\-()\\s]{7,20}$") @RequestParam String contactNumber,
            @RequestParam("birthCertificate") MultipartFile birthCertificate,
            @RequestParam("photo") MultipartFile photo,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) throws IOException {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        Long userId = sessionUser.get().userId();
        try {
            service.ensureNoActiveApplication(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
        form.setUserId(userId);
        form.setUserEmail(sessionUser.get().email());

        NewNicForm saved;
        try {
            saved = service.save(form);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(new ApplicationSubmissionResponse("New NIC application submitted successfully.", saved.getId()));
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
            @Valid @RequestBody StatusUpdateRequest payload,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageApplications(token)) {
            return ResponseEntity.status(403).body("Application review access required");
        }
        try {
            return ResponseEntity.ok(NewNicFormDto.from(service.updateStatus(id, payload.status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
