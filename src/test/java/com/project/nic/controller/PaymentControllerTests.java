package com.project.nic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.nic.model.Payment;
import com.project.nic.model.User;
import com.project.nic.repository.PaymentRecordRepository;
import com.project.nic.repository.PaymentRepository;
import com.project.nic.service.AuthSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        paymentRecordRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void financeCreatePaymentRequiresFinanceRoleAndPersistsPayment() throws Exception {
        Map<String, Object> request = Map.of(
                "serviceType", "new",
                "paymentMethod", "card",
                "amount", 1500.0,
                "status", "completed",
                "userId", 101L,
                "email", "citizen@example.com",
                "customerInfo", "Citizen payment"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Finance access required"));

        mockMvc.perform(post("/api/payments")
                        .header("X-Auth-Token", tokenFor(101L, "citizen@example.com", "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Finance access required"));

        mockMvc.perform(post("/api/payments")
                        .header("X-Auth-Token", tokenFor(501L, "finance@example.com", "FINANCE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.data.serviceType").value("new"))
                .andExpect(jsonPath("$.data.amount").value(1500.0))
                .andExpect(jsonPath("$.data.userId").value(101));

        assertThat(paymentRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getPaymentId()).startsWith("PAY-");
                    assertThat(saved.getServiceType()).isEqualTo("new");
                    assertThat(saved.getPaymentMethod()).isEqualTo("card");
                    assertThat(saved.getAmount()).isEqualTo(1500.0);
                    assertThat(saved.getStatus()).isEqualTo("completed");
                    assertThat(saved.getUserId()).isEqualTo(101L);
                    assertThat(saved.getDate()).isNotNull();
                });
    }

    @Test
    void checkoutPaymentRequiresLoginAndCreatesPaymentRecordForCitizen() throws Exception {
        Map<String, Object> request = Map.of(
                "serviceType", "Renew NIC",
                "paymentMethod", "bank deposit",
                "amount", 2500.0,
                "nic", "200012345678"
        );

        mockMvc.perform(post("/api/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(post("/api/payments/checkout")
                        .header("X-Auth-Token", tokenFor(202L, "payer@example.com", "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(202))
                .andExpect(jsonPath("$.data.email").value("payer@example.com"))
                .andExpect(jsonPath("$.data.serviceType").value("renew"))
                .andExpect(jsonPath("$.data.paymentMethod").value("deposit"))
                .andExpect(jsonPath("$.data.status").value("pending"));

        Payment saved = paymentRepository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo(202L);
        assertThat(saved.getEmail()).isEqualTo("payer@example.com");
        assertThat(saved.getCustomerInfo()).isEqualTo("payer@example.com");
        assertThat(saved.getPaymentId()).startsWith("PAY-");

        assertThat(paymentRecordRepository.findAll())
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getUserId()).isEqualTo("202");
                    assertThat(record.getNicType()).isEqualTo("renew");
                    assertThat(record.getNicReference()).isEqualTo("200012345678");
                    assertThat(record.getAmount()).isEqualTo(2500.0);
                    assertThat(record.getPaymentMethod()).isEqualTo("deposit");
                    assertThat(record.getTransactionId()).isEqualTo(saved.getPaymentId());
                    assertThat(record.getTransactionDate()).isNotNull();
                });
    }

    @Test
    void paymentListRequiresLoginAndScopesResultsByRole() throws Exception {
        paymentRepository.save(payment("PAY-CITIZEN-1", 101L, "citizen@example.com", 1500.0));
        paymentRepository.save(payment("PAY-OTHER-1", 202L, "other@example.com", 2500.0));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(get("/api/payments")
                        .header("X-Auth-Token", tokenFor(101L, "citizen@example.com", "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].paymentId").value("PAY-CITIZEN-1"));

        mockMvc.perform(get("/api/payments/mine")
                        .header("X-Auth-Token", tokenFor(202L, "other@example.com", "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].paymentId").value("PAY-OTHER-1"));

        mockMvc.perform(get("/api/payments")
                        .header("X-Auth-Token", tokenFor(501L, "finance@example.com", "FINANCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    private Payment payment(String paymentId, Long userId, String email, Double amount) {
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setUserId(userId);
        payment.setEmail(email);
        payment.setCustomerInfo(email);
        payment.setServiceType("new");
        payment.setPaymentMethod("card");
        payment.setAmount(amount);
        payment.setStatus("completed");
        return payment;
    }

    private String tokenFor(Long userId, String email, String role) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);
        return authSessionService.createSession(user);
    }
}
