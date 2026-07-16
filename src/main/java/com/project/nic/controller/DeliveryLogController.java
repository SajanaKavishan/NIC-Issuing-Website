package com.project.nic.controller;

import com.project.nic.model.DeliveryLog;
import com.project.nic.service.DeliveryLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/delivery-logs")
@CrossOrigin(origins = "*")
public class DeliveryLogController {

    @Autowired
    private DeliveryLogService deliveryLogService;

    @GetMapping
    public List<DeliveryLog> listAll() {
        return deliveryLogService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryLog> getById(@PathVariable Long id) {
        Optional<DeliveryLog> opt = deliveryLogService.getById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DeliveryLog> create(@RequestBody DeliveryLog log) {
        if (log.getDate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        DeliveryLog saved = deliveryLogService.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryLog> update(@PathVariable Long id, @RequestBody DeliveryLog update) {
        Optional<DeliveryLog> opt = deliveryLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryLog existing = opt.get();
        if (update.getDate() != null) existing.setDate(update.getDate());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        DeliveryLog saved = deliveryLogService.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<DeliveryLog> opt = deliveryLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        deliveryLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

