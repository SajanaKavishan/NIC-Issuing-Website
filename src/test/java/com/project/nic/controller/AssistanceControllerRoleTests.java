package com.project.nic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.nic.model.AssistanceRequest;
import com.project.nic.model.User;
import com.project.nic.repository.AssistanceRequestRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssistanceControllerRoleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssistanceRequestRepository assistanceRequestRepository;

    @Autowired
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        assistanceRequestRepository.deleteAll();
    }

    @Test
    void allRequestsRequiresAssistantOrAdminRole() throws Exception {
        assistanceRequestRepository.save(assistanceRequest(101L, "citizen@example.com", "Need help"));

        mockMvc.perform(get("/api/assistance/all"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assistant access required"));

        mockMvc.perform(get("/api/assistance/all")
                        .header("X-Auth-Token", tokenFor(101L, "citizen@example.com", "CITIZEN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assistant access required"));

        mockMvc.perform(get("/api/assistance/all")
                        .header("X-Auth-Token", tokenFor(201L, "assistant@example.com", "ASSISTANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("citizen@example.com"))
                .andExpect(jsonPath("$.data[0].query").value("Need help"));

        mockMvc.perform(get("/api/assistance/all")
                        .header("X-Auth-Token", tokenFor(1L, "admin@example.com", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("citizen@example.com"));
    }

    @Test
    void replyRequiresAssistantOrAdminRoleAndUpdatesRequest() throws Exception {
        AssistanceRequest request = assistanceRequestRepository.save(
                assistanceRequest(101L, "citizen@example.com", "Need help")
        );

        mockMvc.perform(post("/api/assistance/reply/{id}", request.getId())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Please visit the divisional office."))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Assistant access required"));

        mockMvc.perform(post("/api/assistance/reply/{id}", request.getId())
                        .header("X-Auth-Token", tokenFor(101L, "citizen@example.com", "CITIZEN"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Citizen cannot reply."))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Assistant access required"));

        mockMvc.perform(post("/api/assistance/reply/{id}", request.getId())
                        .header("X-Auth-Token", tokenFor(201L, "assistant@example.com", "ASSISTANT"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Please visit the divisional office."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reply sent successfully"));

        AssistanceRequest updated = assistanceRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("resolved");
        assertThat(updated.getReply()).isEqualTo("Please visit the divisional office.");
    }

    @Test
    void citizenCanUpdateOnlyOwnAssistanceRequest() throws Exception {
        AssistanceRequest request = assistanceRequestRepository.save(
                assistanceRequest(101L, "owner@example.com", "Original query")
        );
        String ownerToken = tokenFor(101L, "owner@example.com", "CITIZEN");
        String otherUserToken = tokenFor(202L, "other@example.com", "CITIZEN");

        mockMvc.perform(put("/api/assistance/request/{id}", request.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "Updated query"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(put("/api/assistance/request/{id}", request.getId())
                        .header("X-Auth-Token", otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "Updated query"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your own assistance requests"));

        mockMvc.perform(put("/api/assistance/request/{id}", request.getId())
                        .header("X-Auth-Token", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "Updated query"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.query").value("Updated query"));

        assertThat(assistanceRequestRepository.findById(request.getId()).orElseThrow().getQuery())
                .isEqualTo("Updated query");
    }

    @Test
    void citizenCanDeleteOnlyOwnAssistanceRequest() throws Exception {
        AssistanceRequest request = assistanceRequestRepository.save(
                assistanceRequest(101L, "owner@example.com", "Delete me")
        );
        String ownerToken = tokenFor(101L, "owner@example.com", "CITIZEN");
        String otherUserToken = tokenFor(202L, "other@example.com", "CITIZEN");

        mockMvc.perform(delete("/api/assistance/request/{id}", request.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login required"));

        mockMvc.perform(delete("/api/assistance/request/{id}", request.getId())
                        .header("X-Auth-Token", otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own assistance requests"));

        mockMvc.perform(delete("/api/assistance/request/{id}", request.getId())
                        .header("X-Auth-Token", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Request deleted successfully"));

        assertThat(assistanceRequestRepository.findById(request.getId())).isEmpty();
    }

    private AssistanceRequest assistanceRequest(Long userId, String email, String query) {
        AssistanceRequest request = new AssistanceRequest();
        request.setUserId(userId);
        request.setEmail(email);
        request.setQuery(query);
        request.setStatus("pending");
        return request;
    }

    private String tokenFor(Long userId, String email, String role) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);
        return authSessionService.createSession(user);
    }
}
