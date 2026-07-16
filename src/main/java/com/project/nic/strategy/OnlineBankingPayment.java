package com.project.nic.strategy;

import com.project.nic.model.Payment;
import com.project.nic.service.PaymentStrategy;

public class OnlineBankingPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Payment payment) {
        // Simulate online banking payment processing logic
        System.out.println("Processing online banking payment for amount: " + payment.getAmount());
        // In a real system, integrate with a bank API for online banking
        payment.setStatus("pending"); // Online banking may require bank confirmation
        return true;
    }
}