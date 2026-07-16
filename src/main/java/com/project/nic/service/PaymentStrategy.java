package com.project.nic.service;

import com.project.nic.model.Payment;

public interface PaymentStrategy {
    boolean processPayment(Payment payment);
}