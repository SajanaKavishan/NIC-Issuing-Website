package com.project.nic.controller;

import com.project.nic.model.RenewNic;
import com.project.nic.service.RenewNicService;
import com.project.nic.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/renew-nic")
public class RenewNicController {
    @Autowired
    private RenewNicService service;

    @PostMapping("/submit")
    public String submitRenewNic(
        @RequestParam String oldNicNumber,
        @RequestParam String birthdate,
        @RequestParam String reason,
        @RequestParam(required = false) String otherReason,
        @RequestParam String contactNumber,
        @RequestParam("birthCertificate") MultipartFile birthCertificate,
        @RequestParam("photo") MultipartFile photo
    ) throws IOException {
        String birthCertPath = FileUploadUtil.saveFile("uploads", birthCertificate);
        String photoPath = FileUploadUtil.saveFile("uploads", photo);

        RenewNic renewNic = new RenewNic();
        renewNic.setOldNicNumber(oldNicNumber);
        renewNic.setBirthdate(LocalDate.parse(birthdate));
        renewNic.setReason(reason);
        renewNic.setOtherReason(otherReason);
        renewNic.setContactNumber(contactNumber);
        renewNic.setBirthCertificatePath(birthCertPath);
        renewNic.setPhotoPath(photoPath);

        service.save(renewNic);
        return "NIC renewal request submitted successfully.";
    }

    @GetMapping("api/renew-nic/all")
    public List<RenewNic> getAllRenewNics() {
        return service.findAll();
    }
}
