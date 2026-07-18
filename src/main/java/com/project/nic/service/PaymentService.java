package com.project.nic.service;

import com.project.nic.model.Payment;
import com.project.nic.model.PaymentRecord;
import com.project.nic.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRecordService paymentRecordService;
    private final NewNicFormService newNicFormService;
    private final RenewNicService renewNicService;
    private final LostNicService lostNicService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentRecordService paymentRecordService,
            NewNicFormService newNicFormService,
            RenewNicService renewNicService,
            LostNicService lostNicService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentRecordService = paymentRecordService;
        this.newNicFormService = newNicFormService;
        this.renewNicService = renewNicService;
        this.lostNicService = lostNicService;
    }
    
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }
    
    public Optional<Payment> getPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
    
    public Payment createPayment(Payment payment) {
        if (payment.getPaymentId() == null || payment.getPaymentId().isBlank()) {
            payment.setPaymentId(generatePaymentId());
        }
        payment.setDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment createCitizenPayment(Payment payment, AuthSessionService.SessionUser sessionUser) {
        payment.setUserId(sessionUser.userId());
        payment.setEmail(firstNonBlank(payment.getEmail(), sessionUser.email()));
        payment.setCustomerInfo(firstNonBlank(payment.getCustomerInfo(), payment.getEmail()));
        payment.setPaymentId(firstNonBlank(payment.getPaymentId(), generatePaymentId()));
        payment.setServiceType(normalizeNicType(payment.getServiceType()));
        payment.setPaymentMethod(normalizePaymentMethod(payment.getPaymentMethod()));
        payment.setStatus(normalizePaymentStatus(payment.getStatus(), payment.getPaymentMethod()));

        Payment saved = createPayment(payment);
        savePaymentRecord(saved, sessionUser);
        return saved;
    }
    
    public Payment updatePayment(Long id, Payment paymentDetails) {
        Optional<Payment> optionalPayment = paymentRepository.findById(id);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            payment.setServiceType(paymentDetails.getServiceType());
            payment.setPaymentMethod(paymentDetails.getPaymentMethod());
            payment.setAmount(paymentDetails.getAmount());
            payment.setStatus(paymentDetails.getStatus());
            if (paymentDetails.getUserId() != null) {
                payment.setUserId(paymentDetails.getUserId());
            }
            // Update new fields if provided
            payment.setNic(paymentDetails.getNic());
            payment.setEmail(paymentDetails.getEmail());
            // Keep customerInfo for backward compatibility
            payment.setCustomerInfo(paymentDetails.getCustomerInfo());
            return paymentRepository.save(payment);
        }
        return null;
    }
    
    public boolean deletePayment(Long id) {
        if (paymentRepository.existsById(id)) {
            paymentRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }
    
    // Statistics methods
    public Double getTotalRevenue() {
        return paymentRepository.findAll().stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();
    }
    
    public Long getPendingPaymentsCount() {
        return paymentRepository.findByStatus("pending").stream().count();
    }
    
    public Long getCompletedPaymentsCount() {
        return paymentRepository.findByStatus("completed").stream().count();
    }

    private void savePaymentRecord(Payment payment, AuthSessionService.SessionUser sessionUser) {
        PaymentRecord record = new PaymentRecord();
        record.setUserId(String.valueOf(sessionUser.userId()));
        record.setNicType(payment.getServiceType());
        record.setNicReference(resolveNicReference(payment));
        record.setAmount(payment.getAmount() == null ? 0 : payment.getAmount());
        record.setPaymentMethod(payment.getPaymentMethod());
        record.setTransactionId(payment.getPaymentId());
        record.setTransactionDate(payment.getDate() == null ? LocalDateTime.now() : payment.getDate());
        paymentRecordService.save(record);
    }

    private String resolveNicReference(Payment payment) {
        if (payment.getNic() != null && !payment.getNic().isBlank()) {
            return payment.getNic().trim();
        }

        Long userId = payment.getUserId();
        if (userId == null) {
            return payment.getPaymentId();
        }

        String type = normalizeNicType(payment.getServiceType());
        if ("new".equals(type)) {
            return newNicFormService.findByUserId(userId).stream()
                    .max((a, b) -> a.getId().compareTo(b.getId()))
                    .map(form -> "NEW-" + String.format("%05d", form.getId()))
                    .orElse(payment.getPaymentId());
        }
        if ("renew".equals(type)) {
            return renewNicService.findByUserId(userId).stream()
                    .max((a, b) -> a.getId().compareTo(b.getId()))
                    .map(form -> firstNonBlank(form.getOldNicNumber(), "REN-" + String.format("%05d", form.getId())))
                    .orElse(payment.getPaymentId());
        }
        if ("lost".equals(type)) {
            return lostNicService.findByUserId(userId).stream()
                    .max((a, b) -> a.getId().compareTo(b.getId()))
                    .map(form -> firstNonBlank(form.getNicNumber(), "LST-" + String.format("%05d", form.getId())))
                    .orElse(payment.getPaymentId());
        }
        return payment.getPaymentId();
    }

    private String generatePaymentId() {
        return "PAY-" + System.currentTimeMillis();
    }

    private String normalizeNicType(String serviceType) {
        String value = serviceType == null ? "new" : serviceType.trim().toLowerCase();
        if (value.contains("renew")) return "renew";
        if (value.contains("lost")) return "lost";
        return "new";
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String value = paymentMethod == null ? "card" : paymentMethod.trim().toLowerCase();
        if (value.contains("deposit")) return "deposit";
        if (value.contains("online")) return "online";
        return "card";
    }

    private String normalizePaymentStatus(String status, String paymentMethod) {
        if (status != null && !status.isBlank()) {
            String value = status.trim().toLowerCase();
            if ("completed".equals(value) || "pending".equals(value) || "failed".equals(value)) {
                return value;
            }
        }
        return "card".equals(normalizePaymentMethod(paymentMethod)) ? "completed" : "pending";
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first.trim();
    }
}
