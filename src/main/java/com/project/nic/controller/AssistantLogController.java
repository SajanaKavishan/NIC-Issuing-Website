package com.project.nic.controller;

import com.project.nic.model.AssistantLog;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.AssistantLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assistant-logs")
public class AssistantLogController {

    @Autowired
    private AssistantLogService assistantLogService;

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManageAssistance(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "ASSISTANT");
    }

    @GetMapping
    public ResponseEntity<?> listAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        return ResponseEntity.ok(assistantLogService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        return opt.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody AssistantLog log,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        if (log.getDate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        AssistantLog saved = assistantLogService.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody AssistantLog update,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        AssistantLog existing = opt.get();
        if (update.getDate() != null) existing.setDate(update.getDate());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        AssistantLog saved = assistantLogService.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        assistantLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

