package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.LogDto;
import com.project.nic.model.AssistantLog;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AssistantLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assistant-logs")
public class AssistantLogController {

    private final AssistantLogService assistantLogService;
    private final AuthAccessService authAccessService;

    public AssistantLogController(AssistantLogService assistantLogService, AuthAccessService authAccessService) {
        this.assistantLogService = assistantLogService;
        this.authAccessService = authAccessService;
    }

    @GetMapping
    public ResponseEntity<?> listAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        return ResponseEntity.ok(assistantLogService.getAll().stream().map(LogDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        return opt.<ResponseEntity<?>>map(log -> ResponseEntity.ok(LogDto.from(log))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody LogDto log,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        if (log.date == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        AssistantLog saved = assistantLogService.save(log.toAssistantLog());
        return ResponseEntity.status(HttpStatus.CREATED).body(LogDto.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody LogDto update,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        AssistantLog existing = opt.get();
        if (update.date != null) existing.setDate(update.date);
        if (update.description != null) existing.setDescription(update.description);
        AssistantLog saved = assistantLogService.save(existing);
        return ResponseEntity.ok(LogDto.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        assistantLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

