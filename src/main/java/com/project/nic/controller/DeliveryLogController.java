package com.project.nic.controller;

import com.project.nic.model.DeliveryLog;
import com.project.nic.service.AuthSessionService;
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

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManageDelivery(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "DELIVERY");
    }

    @GetMapping
    public ResponseEntity<?> listAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        return ResponseEntity.ok(deliveryLogService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        Optional<DeliveryLog> opt = deliveryLogService.getById(id);
        return opt.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody DeliveryLog log,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        if (log.getDate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        DeliveryLog saved = deliveryLogService.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody DeliveryLog update,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        Optional<DeliveryLog> opt = deliveryLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        DeliveryLog existing = opt.get();
        if (update.getDate() != null) existing.setDate(update.getDate());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        DeliveryLog saved = deliveryLogService.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        Optional<DeliveryLog> opt = deliveryLogService.getById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        deliveryLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

