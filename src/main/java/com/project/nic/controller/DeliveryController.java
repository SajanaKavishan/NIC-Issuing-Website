package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.DeliveryDto;
import com.project.nic.model.Delivery;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private AuthAccessService authAccessService;

    @GetMapping("/nics")
    public ResponseEntity<?> getAllDeliveries(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        logger.info("GET /api/delivery/nics called with dateRange='{}', deliveryMethod='{}', search='{}',",
                dateRange, deliveryMethod, search);
        return ResponseEntity.ok(deliveryService.getAllDeliveries(dateRange, deliveryMethod, search).stream().map(DeliveryDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/nics/all")
    public ResponseEntity<?> getAllDeliveries(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        return ResponseEntity.ok(deliveryService.getAll().stream().map(DeliveryDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/nics/{nic}")
    public ResponseEntity<?> getDeliveryByNic(
            @PathVariable String nic,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        Optional<Delivery> delivery = deliveryService.getDeliveryByNic(nic);
        return delivery.<ResponseEntity<?>>map(item -> ResponseEntity.ok(DeliveryDto.from(item))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/nics")
    public ResponseEntity<?> createDelivery(
            @RequestBody DeliveryDto deliveryRequest,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        return ResponseEntity.ok(DeliveryDto.from(deliveryService.saveDelivery(deliveryRequest.toEntity())));
    }

    @PutMapping("/nics/{nic}")
    public ResponseEntity<?> updateMethodAndStatus(
            @PathVariable String nic,
            @RequestBody DeliveryDto update,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        logger.info("PUT /api/delivery/nics/{} called with payload method='{}', status='{}'", nic, update.method, update.status);
        Delivery delivery = deliveryService.getDeliveryByNic(nic).orElseThrow();
        String method = update.method == null ? null : update.method.trim();
        String status = update.status == null ? null : update.status.trim();
        if (method != null && !method.isBlank()) {
            delivery.setMethod(method);
        }
        if (status != null && !status.isBlank()) {
            delivery.setStatus(status);
        }
        Delivery saved = deliveryService.saveDelivery(delivery);
        logger.info("Saved delivery for nic='{}' with method='{}', status='{}'", nic, saved.getMethod(), saved.getStatus());
        return ResponseEntity.ok(DeliveryDto.from(saved));
    }

    @DeleteMapping("/nics/{nic}")
    public ResponseEntity<?> deleteDelivery(
            @PathVariable String nic,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageDelivery(token)) {
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
        if (!authAccessService.canManageDelivery(token)) {
            return ResponseEntity.status(403).body("Delivery access required");
        }
        LocalDate start = LocalDate.parse(startDate);
        return ResponseEntity.ok(deliveryService.getWeeklyDeliveries(start).stream().map(DeliveryDto::from).collect(Collectors.toList()));
    }
}
