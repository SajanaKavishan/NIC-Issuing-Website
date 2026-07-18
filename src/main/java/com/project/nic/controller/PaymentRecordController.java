package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.PaymentRecordDto;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.PaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment-records")
public class PaymentRecordController {

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Autowired
    private AuthSessionService authSessionService;

    @GetMapping
    public ResponseEntity<?> getPaymentRecords(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authSessionService.findByToken(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        if (authSessionService.hasAnyRole(token, "ADMIN", "FINANCE")) {
            return ResponseEntity.ok(paymentRecordService.getAll().stream().map(PaymentRecordDto::from).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(paymentRecordService.getByUserId(sessionUser.get().userId()).stream().map(PaymentRecordDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyPaymentRecords(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        Optional<AuthSessionService.SessionUser> sessionUser = authSessionService.findByToken(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }
        return ResponseEntity.ok(paymentRecordService.getByUserId(sessionUser.get().userId()).stream().map(PaymentRecordDto::from).collect(Collectors.toList()));
    }
}
