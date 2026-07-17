package com.project.nic.controller;

import com.project.nic.model.Delivery;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManageDelivery(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "DELIVERY");
    }

    @GetMapping("/nics")
    public ResponseEntity<?> getAllDeliveries(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        logger.info("GET /api/delivery/nics called with dateRange='{}', deliveryMethod='{}', search='{}',",
                dateRange, deliveryMethod, search);
        return ResponseEntity.ok(deliveryService.getAllDeliveries(dateRange, deliveryMethod, search));
    }

    @GetMapping("/nics/all")
    public ResponseEntity<?> getAllDeliveries(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        return ResponseEntity.ok(deliveryService.getAll());
    }

    @GetMapping("/nics/{nic}")
    public ResponseEntity<?> getDeliveryByNic(
            @PathVariable String nic,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        Optional<Delivery> delivery = deliveryService.getDeliveryByNic(nic);
        return delivery.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/nics")
    public ResponseEntity<?> createDelivery(
            @RequestBody Delivery delivery,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        return ResponseEntity.ok(deliveryService.saveDelivery(delivery));
    }

    @PutMapping("/nics/{nic}")
    public ResponseEntity<?> updateMethodAndStatus(
            @PathVariable String nic,
            @RequestBody Delivery update,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        logger.info("PUT /api/delivery/nics/{} called with payload method='{}', status='{}'", nic, update.getMethod(), update.getStatus());
        Delivery delivery = deliveryService.getDeliveryByNic(nic).orElseThrow();
        String method = update.getMethod() == null ? null : update.getMethod().trim();
        String status = update.getStatus() == null ? null : update.getStatus().trim();
        if (method != null && !method.isBlank()) {
            delivery.setMethod(method);
        }
        if (status != null && !status.isBlank()) {
            delivery.setStatus(status);
        }
        Delivery saved = deliveryService.saveDelivery(delivery);
        logger.info("Saved delivery for nic='{}' with method='{}', status='{}'", nic, saved.getMethod(), saved.getStatus());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/nics/{nic}")
    public ResponseEntity<?> deleteDelivery(
            @PathVariable String nic,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        logger.info("DELETE /api/delivery/nics/{} called", nic);
        boolean deleted = deliveryService.deleteByNic(nic);
        if (deleted) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/weekly-report")
    public ResponseEntity<?> getWeeklyReport(
            @RequestParam String startDate,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        LocalDate start = LocalDate.parse(startDate);
        return ResponseEntity.ok(deliveryService.getWeeklyDeliveries(start));
    }
}
