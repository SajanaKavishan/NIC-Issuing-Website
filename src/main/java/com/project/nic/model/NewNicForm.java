package com.project.nic.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "newnic")
public class NewNicForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameWithInitials;
    private String gender;
    private int age;
    private String civilStatus;
    private String profession;
    private LocalDate birthdate;
    private String address;
    private String contactNumber;

    private String birthCertificatePath;
    private String photoPath;

    // Getters
    public Long getId() {
        return id;
    }

    public String getNameWithInitials() {
        return nameWithInitials;
    }

    public String getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }

    public String getCivilStatus() {
        return civilStatus;
    }

    public String getProfession() {
        return profession;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public String getAddress() {
        return address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getBirthCertificatePath() {
        return birthCertificatePath;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    // Setters
    public void setNameWithInitials(String nameWithInitials) {
        this.nameWithInitials = nameWithInitials;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setCivilStatus(String civilStatus) {
        this.civilStatus = civilStatus;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setBirthCertificatePath(String birthCertificatePath) {
        this.birthCertificatePath = birthCertificatePath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
