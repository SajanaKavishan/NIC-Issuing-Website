package com.project.nic.strategy;

import com.project.nic.model.Payment;
import com.project.nic.service.PaymentStrategy;

public class OnlineBankingPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Payment payment) {
        // Integrate a bank API here when live online banking confirmation is configured.
        payment.setStatus("pending"); // Online banking may require bank confirmation
        return true;
    }
}
