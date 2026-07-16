package com.project.nic.service;

import com.project.nic.model.Payment;
import com.project.nic.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }
    
    public Optional<Payment> getPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
    
    public Payment createPayment(Payment payment) {
        payment.setDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }
    
    public Payment updatePayment(Long id, Payment paymentDetails) {
        Optional<Payment> optionalPayment = paymentRepository.findById(id);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            payment.setServiceType(paymentDetails.getServiceType());
            payment.setPaymentMethod(paymentDetails.getPaymentMethod());
            payment.setAmount(paymentDetails.getAmount());
            payment.setStatus(paymentDetails.getStatus());
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
}