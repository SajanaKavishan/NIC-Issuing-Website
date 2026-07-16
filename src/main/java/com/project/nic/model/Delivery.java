package com.project.nic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    private String nic;
    @Column(name = "app_id")
    private Long appId; // Foreign key to new_nic.id
    private String recipient;
    @Column(name = "delivery_date")
    private java.time.LocalDate deliveryDate;
    private String method;
    private String status;
    private String address;

    // Default constructor
    public Delivery() {}

    // Parameterized constructor
    public Delivery(String nic, Long appId, String recipient, java.time.LocalDate deliveryDate, String method, String status, String address) {
        this.nic = nic;
        this.appId = appId;
        this.recipient = recipient;
        this.deliveryDate = deliveryDate;
        this.method = method;
        this.status = status;
        this.address = address;
    }

    // Getters and Setters
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public java.time.LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(java.time.LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}