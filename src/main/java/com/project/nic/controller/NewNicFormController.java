package com.project.nic.controller;

import com.project.nic.model.NewNicForm;
import com.project.nic.service.NewNicFormService;
import com.project.nic.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/new-nic")
public class NewNicFormController {

    @Autowired
    private NewNicFormService service;

    @PostMapping("/submit")
    public String submitForm(
            @RequestParam String nameWithInitials,
            @RequestParam String gender,
            @RequestParam int age,
            @RequestParam String civilStatus,
            @RequestParam String profession,
            @RequestParam String birthdate,
            @RequestParam String address,
            @RequestParam String contactNumber,
            @RequestParam("birthCertificate") MultipartFile birthCertificate,
            @RequestParam("photo") MultipartFile photo
    ) throws IOException {

        String birthCertPath = FileUploadUtil.saveFile("uploads", birthCertificate);
        String photoPath = FileUploadUtil.saveFile("uploads", photo);

        NewNicForm form = new NewNicForm();
        form.setNameWithInitials(nameWithInitials);
        form.setGender(gender);
        form.setAge(age);
        form.setCivilStatus(civilStatus);
        form.setProfession(profession);
        form.setBirthdate(LocalDate.parse(birthdate));
        form.setAddress(address);
        form.setContactNumber(contactNumber);
        form.setBirthCertificatePath(birthCertPath);
        form.setPhotoPath(photoPath);

        service.save(form);
        return "New NIC application submitted successfully.";
    }
}
