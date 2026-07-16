package com.project.nic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String paymentId;
    
    private LocalDateTime date;
    private String serviceType;
    private String paymentMethod;
    private Double amount;
    private String status;

    // New fields to store NIC and Email separately
    private String nic; // NIC from previous step
    private String email; // Email from paymentGateway page

    // Keep for backward compatibility if any existing data used it
    private String customerInfo;
    
    // Constructors
    public Payment() {}
    
    public Payment(String paymentId, LocalDateTime date, String serviceType, 
                   String paymentMethod, Double amount, String status, String customerInfo) {
        this.paymentId = paymentId;
        this.date = date;
        this.serviceType = serviceType;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = status;
        this.customerInfo = customerInfo;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getCustomerInfo() { return customerInfo; }
    public void setCustomerInfo(String customerInfo) { this.customerInfo = customerInfo; }
}