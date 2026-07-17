package com.project.nic.strategy;

import com.project.nic.model.Payment;
import com.project.nic.service.PaymentStrategy;

public class CreditCardPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Payment payment) {
        // Integrate a payment gateway here when live card processing is configured.
        payment.setStatus("completed");
        return true;
    }
}
