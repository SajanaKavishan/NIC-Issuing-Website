package com.project.nic.controller;

import com.project.nic.model.AssistantLog;
import com.project.nic.service.AssistantLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assistant-logs")
@CrossOrigin(origins = "*")
public class AssistantLogController {

    @Autowired
    private AssistantLogService assistantLogService;

    @GetMapping
    public List<AssistantLog> listAll() {
        return assistantLogService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssistantLog> getById(@PathVariable Long id) {
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AssistantLog> create(@RequestBody AssistantLog log) {
        if (log.getDate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        AssistantLog saved = assistantLogService.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssistantLog> update(@PathVariable Long id, @RequestBody AssistantLog update) {
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        AssistantLog existing = opt.get();
        if (update.getDate() != null) existing.setDate(update.getDate());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        AssistantLog saved = assistantLogService.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<AssistantLog> opt = assistantLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        assistantLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

