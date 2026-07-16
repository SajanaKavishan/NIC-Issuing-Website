package com.project.nic.controller;

import com.project.nic.model.Delivery;
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
@CrossOrigin(origins = "*")
public class DeliveryController {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private DeliveryService deliveryService;

    @GetMapping("/nics")
    public List<Delivery> getAllDeliveries(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String search) {
        logger.info("GET /api/delivery/nics called with dateRange='{}', deliveryMethod='{}', search='{}',",
                dateRange, deliveryMethod, search);
        return deliveryService.getAllDeliveries(dateRange, deliveryMethod, search);
    }

    @GetMapping("/nics/all")
    public List<Delivery> getAllDeliveries() {
        return deliveryService.getAll();
    }

    @GetMapping("/nics/{nic}")
    public Optional<Delivery> getDeliveryByNic(@PathVariable String nic) {
        return deliveryService.getDeliveryByNic(nic);
    }

    @PostMapping("/nics")
    public Delivery createDelivery(@RequestBody Delivery delivery) {
        return deliveryService.saveDelivery(delivery);
    }

    @PutMapping("/nics/{nic}")
    public Delivery updateMethodAndStatus(@PathVariable String nic, @RequestBody Delivery update) {
        logger.info("PUT /api/delivery/nics/{} called with payload method='{}', status='{}'", nic, update.getMethod(), update.getStatus());
        Delivery delivery = deliveryService.getDeliveryByNic(nic).orElseThrow();
        String method = update.getMethod() == null ? null : update.getMethod().trim();
        String status = update.getStatus() == null ? null : update.getStatus().trim();
        delivery.setMethod(method);
        delivery.setStatus(status);
        Delivery saved = deliveryService.saveDelivery(delivery);
        logger.info("Saved delivery for nic='{}' with method='{}', status='{}'", nic, saved.getMethod(), saved.getStatus());
        return saved;
    }

    @DeleteMapping("/nics/{nic}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable String nic) {
        logger.info("DELETE /api/delivery/nics/{} called", nic);
        boolean deleted = deliveryService.deleteByNic(nic);
        if (deleted) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/weekly-report")
    public List<Delivery> getWeeklyReport(@RequestParam String startDate) {
        LocalDate start = LocalDate.parse(startDate);
        return deliveryService.getWeeklyDeliveries(start);
    }
}