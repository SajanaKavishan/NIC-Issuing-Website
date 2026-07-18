package com.project.nic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.nic.model.LostNic;
import com.project.nic.model.NewNicForm;
import com.project.nic.model.RenewNic;
import com.project.nic.model.User;
import com.project.nic.repository.LostNicRepository;
import com.project.nic.repository.NewNicFormRepository;
import com.project.nic.repository.RenewNicRepository;
import com.project.nic.service.AuthSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NicApplicationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewNicFormRepository newNicFormRepository;

    @Autowired
    private RenewNicRepository renewNicRepository;

    @Autowired
    private LostNicRepository lostNicRepository;

    @Autowired
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        lostNicRepository.deleteAll();
        renewNicRepository.deleteAll();
        newNicFormRepository.deleteAll();
    }

    @Test
    void newNicSubmitRequiresLoginAndSavesCitizenApplication() throws Exception {
        mockMvc.perform(newNicSubmitRequest(null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(newNicSubmitRequest(tokenFor(101L, "citizen@example.com", "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("New NIC application submitted successfully."));

        assertThat(newNicFormRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getUserId()).isEqualTo(101L);
                    assertThat(saved.getUserEmail()).isEqualTo("citizen@example.com");
                    assertThat(saved.getNameWithInitials()).isEqualTo("A B Citizen");
                    assertThat(saved.getBirthdate()).isEqualTo(LocalDate.of(2000, 1, 2));
                    assertThat(saved.getStatus()).isEqualTo("PENDING");
                    assertThat(saved.getBirthCertificatePath()).endsWith(".pdf");
                    assertThat(saved.getPhotoPath()).endsWith(".jpg");
                });
    }

    @Test
    void renewNicSubmitRequiresLoginAndSavesCitizenApplication() throws Exception {
        mockMvc.perform(renewNicSubmitRequest(null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(renewNicSubmitRequest(tokenFor(102L, "renew@example.com", "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("NIC renewal request submitted successfully."));

        assertThat(renewNicRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getUserId()).isEqualTo(102L);
                    assertThat(saved.getUserEmail()).isEqualTo("renew@example.com");
                    assertThat(saved.getOldNicNumber()).isEqualTo("200012345678");
                    assertThat(saved.getReason()).isEqualTo("Damaged");
                    assertThat(saved.getStatus()).isEqualTo("PENDING");
                    assertThat(saved.getBirthCertificatePath()).endsWith(".pdf");
                    assertThat(saved.getPhotoPath()).endsWith(".jpg");
                });
    }

    @Test
    void lostNicSubmitRequiresLoginAndSavesCitizenReport() throws Exception {
        mockMvc.perform(lostNicSubmitRequest(null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(lostNicSubmitRequest(tokenFor(103L, "lost@example.com", "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lost NIC report submitted successfully."));

        assertThat(lostNicRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getUserId()).isEqualTo(103L);
                    assertThat(saved.getUserEmail()).isEqualTo("lost@example.com");
                    assertThat(saved.getNicNumber()).isEqualTo("991234567V");
                    assertThat(saved.getLostDate()).isEqualTo(LocalDate.of(2026, 1, 10));
                    assertThat(saved.getStatus()).isEqualTo("PENDING");
                    assertThat(saved.getBirthCertificatePath()).endsWith(".pdf");
                    assertThat(saved.getPoliceReportPath()).endsWith(".pdf");
                });
    }

    @Test
    void newNicStatusUpdateRequiresApplicationReviewerRoleAndNormalizesStatus() throws Exception {
        NewNicForm application = newNicFormRepository.save(newNicApplication());

        mockMvc.perform(put("/api/new-nic/{id}/status", application.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PROCESSING")))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Application review access required"));

        mockMvc.perform(put("/api/new-nic/{id}/status", application.getId())
                        .header("X-Auth-Token", tokenFor(101L, "citizen@example.com", "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PROCESSING")))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Application review access required"));

        mockMvc.perform(put("/api/new-nic/{id}/status", application.getId())
                        .header("X-Auth-Token", tokenFor(201L, "pro@example.com", "PRO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PROCESSING")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        assertThat(newNicFormRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo("PROCESSING");
    }

    @Test
    void renewNicStatusUpdateRequiresApplicationReviewerRoleAndRejectsInvalidStatus() throws Exception {
        RenewNic application = renewNicRepository.save(renewNicApplication());

        mockMvc.perform(put("/api/renew-nic/{id}/status", application.getId())
                        .header("X-Auth-Token", tokenFor(101L, "citizen@example.com", "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED")))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Application review access required"));

        mockMvc.perform(put("/api/renew-nic/{id}/status", application.getId())
                        .header("X-Auth-Token", tokenFor(1L, "admin@example.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(put("/api/renew-nic/{id}/status", application.getId())
                        .header("X-Auth-Token", tokenFor(1L, "admin@example.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "completed")))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid status: completed"));

        assertThat(renewNicRepository.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo("APPROVED");
    }

    @Test
    void lostNicStatusUpdateRequiresRecoveryRoleAndNormalizesStatus() throws Exception {
        LostNic report = lostNicRepository.save(lostNicReport());

        mockMvc.perform(put("/api/lost-nic/{id}/status", report.getId())
                        .header("X-Auth-Token", tokenFor(201L, "pro@example.com", "PRO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELIVERED")))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Recovery access required"));

        mockMvc.perform(put("/api/lost-nic/{id}/status", report.getId())
                        .header("X-Auth-Token", tokenFor(301L, "recovery@example.com", "RECOVERY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELIVERED")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        assertThat(lostNicRepository.findById(report.getId()).orElseThrow().getStatus())
                .isEqualTo("DELIVERED");
    }

    private MockHttpServletRequestBuilder newNicSubmitRequest(String token) {
        var request = multipart("/api/new-nic/submit")
                .file(pdfFile("birthCertificate"))
                .file(jpgFile("photo"))
                .param("nameWithInitials", "A B Citizen")
                .param("gender", "Female")
                .param("age", "26")
                .param("civilStatus", "Single")
                .param("profession", "Engineer")
                .param("birthdate", "2000-01-02")
                .param("address", "123 Main Street")
                .param("contactNumber", "0771234567");
        if (token != null) {
            request.header("X-Auth-Token", token);
        }
        return request;
    }

    private MockHttpServletRequestBuilder renewNicSubmitRequest(String token) {
        var request = multipart("/api/renew-nic/submit")
                .file(pdfFile("birthCertificate"))
                .file(jpgFile("photo"))
                .param("oldNicNumber", "200012345678")
                .param("birthdate", "2000-01-02")
                .param("reason", "Damaged")
                .param("contactNumber", "0771234567");
        if (token != null) {
            request.header("X-Auth-Token", token);
        }
        return request;
    }

    private MockHttpServletRequestBuilder lostNicSubmitRequest(String token) {
        var request = multipart("/api/lost-nic/submit")
                .file(pdfFile("birthCertificate"))
                .file(pdfFile("policeReport"))
                .param("nicNumber", "991234567V")
                .param("lostDate", "2026-01-10")
                .param("contactNumber", "0771234567");
        if (token != null) {
            request.header("X-Auth-Token", token);
        }
        return request;
    }

    private MockMultipartFile pdfFile(String name) {
        return new MockMultipartFile(name, name + ".pdf", "application/pdf", "%PDF-1.4 test".getBytes());
    }

    private MockMultipartFile jpgFile(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private NewNicForm newNicApplication() {
        NewNicForm form = new NewNicForm();
        form.setNameWithInitials("A B Citizen");
        form.setGender("Female");
        form.setAge(26);
        form.setCivilStatus("Single");
        form.setProfession("Engineer");
        form.setBirthdate(LocalDate.of(2000, 1, 2));
        form.setAddress("123 Main Street");
        form.setContactNumber("0771234567");
        form.setBirthCertificatePath("birth.pdf");
        form.setPhotoPath("photo.jpg");
        form.setUserId(101L);
        form.setUserEmail("citizen@example.com");
        form.setStatus("PENDING");
        return form;
    }

    private RenewNic renewNicApplication() {
        RenewNic form = new RenewNic();
        form.setOldNicNumber("200012345678");
        form.setBirthdate(LocalDate.of(2000, 1, 2));
        form.setReason("Damaged");
        form.setContactNumber("0771234567");
        form.setBirthCertificatePath("birth.pdf");
        form.setPhotoPath("photo.jpg");
        form.setUserId(102L);
        form.setUserEmail("renew@example.com");
        form.setStatus("PENDING");
        return form;
    }

    private LostNic lostNicReport() {
        LostNic report = new LostNic();
        report.setNicNumber("991234567V");
        report.setLostDate(LocalDate.of(2026, 1, 10));
        report.setContactNumber("0771234567");
        report.setBirthCertificatePath("birth.pdf");
        report.setPoliceReportPath("police.pdf");
        report.setUserId(103L);
        report.setUserEmail("lost@example.com");
        report.setStatus("PENDING");
        return report;
    }

    private String tokenFor(Long userId, String email, String role) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);
        return authSessionService.createSession(user);
    }
}
