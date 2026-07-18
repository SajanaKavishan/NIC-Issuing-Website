package com.project.nic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.nic.dto.ApiDtos.UserRequest;
import com.project.nic.model.User;
import com.project.nic.repository.UserRepository;
import com.project.nic.service.AuthSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionService authSessionService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void signupCreatesCitizenWithNormalizedEmailAndHashedPassword() throws Exception {
        UserRequest request = userRequest("Ada", "Lovelace", "ADA@Example.COM", "secret123");

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        Optional<User> savedUser = userRepository.findByEmail("ada@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getFirstName()).isEqualTo("Ada");
        assertThat(savedUser.get().getLastName()).isEqualTo("Lovelace");
        assertThat(savedUser.get().getRole()).isEqualTo("CITIZEN");
        assertThat(savedUser.get().getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", savedUser.get().getPassword())).isTrue();
    }

    @Test
    void signupRejectsDuplicateEmailIgnoringCaseAndWhitespace() throws Exception {
        userRepository.save(existingUser("grace@example.com", "password1"));

        UserRequest request = userRequest("Grace", "Hopper", "GRACE@example.com", "password2");

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void loginReturnsTokenAndUserDetailsForValidCredentials() throws Exception {
        User user = userRepository.save(existingUser("alan@example.com", "password1"));
        UserRequest request = userRequest(null, null, "alan@example.com", "password1");

        String responseBody = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                .andExpect(jsonPath("$.data.user.email").value("alan@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("CITIZEN"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).get("data").get("token").asText();
        assertThat(authSessionService.findByToken(token))
                .hasValueSatisfying(sessionUser -> {
                    assertThat(sessionUser.userId()).isEqualTo(user.getId());
                    assertThat(sessionUser.email()).isEqualTo("alan@example.com");
                    assertThat(sessionUser.role()).isEqualTo("CITIZEN");
                });
    }

    @Test
    void loginRejectsInvalidCredentialsWithoutToken() throws Exception {
        userRepository.save(existingUser("katherine@example.com", "correct-password"));
        UserRequest request = userRequest(null, null, "katherine@example.com", "wrong-password");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request failed"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void sessionReturnsCurrentUserForLoginToken() throws Exception {
        userRepository.save(existingUser("dorothy@example.com", "password1"));
        UserRequest loginRequest = userRequest(null, null, "dorothy@example.com", "password1");

        String responseBody = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(responseBody).get("data").get("token").asText();

        mockMvc.perform(get("/api/users/session")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("dorothy@example.com"))
                .andExpect(jsonPath("$.data.role").value("CITIZEN"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    private UserRequest userRequest(String firstName, String lastName, String email, String password) {
        UserRequest request = new UserRequest();
        request.firstName = firstName;
        request.lastName = lastName;
        request.email = email;
        request.password = password;
        return request;
    }

    private User existingUser(String email, String password) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("CITIZEN");
        return user;
    }
}
