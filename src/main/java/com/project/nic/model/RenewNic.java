
package com.project.nic.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "renewnic")
public class RenewNic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String oldNicNumber;
    private LocalDate birthdate;
    private String reason;
    private String otherReason;
    private String contactNumber;
    private String birthCertificatePath;
    private String photoPath;

    public RenewNic() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOldNicNumber() {
        return oldNicNumber;
    }

    public void setOldNicNumber(String oldNicNumber) {
        this.oldNicNumber = oldNicNumber;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOtherReason() {
        return otherReason;
    }

    public void setOtherReason(String otherReason) {
        this.otherReason = otherReason;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getBirthCertificatePath() {
        return birthCertificatePath;
    }

    public void setBirthCertificatePath(String birthCertificatePath) {
        this.birthCertificatePath = birthCertificatePath;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
