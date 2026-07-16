package com.project.nic.strategy;

import com.project.nic.model.Payment;
import com.project.nic.service.PaymentStrategy;

public class CreditCardPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Payment payment) {
        // Simulate credit card payment processing logic
        System.out.println("Processing credit card payment for amount: " + payment.getAmount());
        // In a real system, integrate with a payment gateway like Stripe or Visa
        payment.setStatus("completed");
        return true;
    }
}