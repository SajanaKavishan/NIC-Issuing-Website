package com.project.nic.strategy;

import com.project.nic.model.Payment;
import com.project.nic.service.PaymentStrategy;

public class BankDepositPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Payment payment) {
        // Simulate bank deposit payment processing logic
        System.out.println("Processing bank deposit payment for amount: " + payment.getAmount());
        // In a real system, this might involve generating a deposit slip or awaiting manual verification
        payment.setStatus("pending"); // Bank deposits typically require manual verification
        return true;
    }
}