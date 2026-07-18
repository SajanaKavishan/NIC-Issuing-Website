package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.PaymentDto;
import com.project.nic.model.Payment;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManagePayments(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "FINANCE");
    }

    private boolean isLoggedIn(String token) {
        return authSessionService.findByToken(token).isPresent();
    }
    
    @GetMapping
    public ResponseEntity<?> getAllPayments(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authSessionService.findByToken(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        if (!canManagePayments(token)) {
            return ResponseEntity.ok(paymentService.getPaymentsByUserId(sessionUser.get().userId()).stream().map(PaymentDto::from).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(paymentService.getAllPayments().stream().map(PaymentDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyPayments(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authSessionService.findByToken(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(sessionUser.get().userId()).stream().map(PaymentDto::from).collect(Collectors.toList()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManagePayments(token)) {
            return ResponseEntity.status(403).body("Finance access required");
        }
        Optional<Payment> payment = paymentService.getPaymentById(id);
        return payment.<ResponseEntity<?>>map(item -> ResponseEntity.ok(PaymentDto.from(item))).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestBody PaymentDto paymentRequest,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManagePayments(token)) {
            return ResponseEntity.status(403).body("Finance access required");
        }
        Payment payment = paymentRequest.toEntity();
        // Generate payment ID if not provided
        if (payment.getPaymentId() == null || payment.getPaymentId().isEmpty()) {
            payment.setPaymentId("PAY-" + System.currentTimeMillis());
        }
        return ResponseEntity.ok(PaymentDto.from(paymentService.createPayment(payment)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutPayment(
            @RequestBody PaymentDto paymentRequest,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        Optional<AuthSessionService.SessionUser> sessionUser = authSessionService.findByToken(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(PaymentDto.from(paymentService.createCitizenPayment(paymentRequest.toEntity(), sessionUser.get())));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePayment(
            @PathVariable Long id,
            @RequestBody PaymentDto paymentDetails,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManagePayments(token)) {
            return ResponseEntity.status(403).body("Finance access required");
        }
        Payment updatedPayment = paymentService.updatePayment(id, paymentDetails.toEntity());
        return updatedPayment != null ? ResponseEntity.ok(PaymentDto.from(updatedPayment)) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManagePayments(token)) {
            return ResponseEntity.status(403).body("Finance access required");
        }
        boolean deleted = paymentService.deletePayment(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getPaymentStatistics(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManagePayments(token)) {
            return ResponseEntity.status(403).body("Finance access required");
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", paymentService.getTotalRevenue());
        stats.put("pendingPayments", paymentService.getPendingPaymentsCount());
        stats.put("completedPayments", paymentService.getCompletedPaymentsCount());
        return ResponseEntity.ok(stats);
    }
}
