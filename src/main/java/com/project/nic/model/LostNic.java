package com.project.nic.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "lostnic")
public class LostNic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String nicNumber;
    private LocalDate lostDate;
    private String contactNumber;
    private String address;
    private String birthCertificatePath;
    private String policeReportPath;

    // New status field: PENDING / PROCESSING / APPROVED / REJECTED / DELIVERED
    private String status;
    private Long userId;
    private String userEmail;

    public LostNic() {
        // default status for new submissions
        this.status = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public void setNicNumber(String nicNumber) {
        this.nicNumber = nicNumber;
    }

    public LocalDate getLostDate() {
        return lostDate;
    }

    public void setLostDate(LocalDate lostDate) {
        this.lostDate = lostDate;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthCertificatePath() {
        return birthCertificatePath;
    }

    public void setBirthCertificatePath(String birthCertificatePath) {
        this.birthCertificatePath = birthCertificatePath;
    }

    public String getPoliceReportPath() {
        return policeReportPath;
    }

    public void setPoliceReportPath(String policeReportPath) {
        this.policeReportPath = policeReportPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
