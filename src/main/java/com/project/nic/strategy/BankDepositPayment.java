package com.project.nic.strategy;

import com.project.nic.model.Payment;
import com.project.nic.service.PaymentStrategy;

public class BankDepositPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(Payment payment) {
        // Bank deposits require manual verification before completion.
        payment.setStatus("pending"); // Bank deposits typically require manual verification
        return true;
    }
}
