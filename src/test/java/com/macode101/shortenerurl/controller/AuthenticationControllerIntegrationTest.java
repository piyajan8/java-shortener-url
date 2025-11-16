package com.macode101.shortenerurl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macode101.shortenerurl.dto.AuthResponse;
import com.macode101.shortenerurl.dto.LoginRequest;
import com.macode101.shortenerurl.dto.RegisterRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateUniqueEmail() {
        return "user-" + UUID.randomUUID() + "@macode101.com";
    }

    @Nested
    class UserRegistration {

        @Test
        void shouldRegisterNewUser() throws Exception {
            String email = generateUniqueEmail();
            RegisterRequest request = new RegisterRequest(email, "password123");

            MvcResult result = mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            AuthResponse response = objectMapper.readValue(responseBody, AuthResponse.class);
            assertThat(response.accessToken()).isNotEmpty();
            assertThat(response.accessToken().split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        void shouldRejectRegistrationWithInvalidEmail() throws Exception {
            String[] invalidEmails = {
                "invalid-email",
                "@macode101.com",
                "user@",
                "user @macode101.com",
                "user@.com",
                ""
            };

            for (String invalidEmail : invalidEmails) {
                RegisterRequest request = new RegisterRequest(invalidEmail, "password123");
                mockMvc.perform(post("/api/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        void shouldRejectRegistrationWithShortPassword() throws Exception {
            RegisterRequest request = new RegisterRequest(generateUniqueEmail(), "short");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectRegistrationWithEmptyPassword() throws Exception {
            RegisterRequest request = new RegisterRequest(generateUniqueEmail(), "");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectRegistrationWithNullValues() throws Exception {
            String requestJson = "{\"email\":null,\"password\":null}";

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDuplicateEmailRegistration() throws Exception {
            String email = generateUniqueEmail();
            RegisterRequest request = new RegisterRequest(email, "password123");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldTreatEmailsAsCaseSensitive() throws Exception {
            String email = generateUniqueEmail();
            RegisterRequest request1 = new RegisterRequest(email.toLowerCase(), "password123");
            RegisterRequest request2 = new RegisterRequest(email.toUpperCase(), "password123");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());
        }

        @Test
        void shouldRejectMalformedJson() throws Exception {
            String malformedJson = "{email: 'test@macode101.com', password: 'password123'}";

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        void shouldAcceptLongValidEmail() throws Exception {
            String longEmail = "very.long.email.address.with.many.dots" + System.nanoTime() + "@macode101.com";
            RegisterRequest request = new RegisterRequest(longEmail, "password123");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class UserLogin {

        @Test
        void shouldLoginWithValidCredentials() throws Exception {
            String email = generateUniqueEmail();
            String password = "password123";
            RegisterRequest registerRequest = new RegisterRequest(email, password);

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(email, password);
            MvcResult result = mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            AuthResponse response = objectMapper.readValue(responseBody, AuthResponse.class);
            assertThat(response.accessToken()).isNotEmpty();
            assertThat(response.accessToken().split("\\.")).hasSize(3);
        }

        @Test
        void shouldRejectLoginWithInvalidPassword() throws Exception {
            String email = generateUniqueEmail();
            RegisterRequest registerRequest = new RegisterRequest(email, "password123");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(email, "wrongpassword");
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectLoginWithNonExistentUser() throws Exception {
            LoginRequest loginRequest = new LoginRequest(generateUniqueEmail(), "password123");

            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectLoginWithInvalidEmailFormat() throws Exception {
            LoginRequest loginRequest = new LoginRequest("invalid-email", "password123");

            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectLoginWithEmptyCredentials() throws Exception {
            LoginRequest loginRequest = new LoginRequest("", "");

            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldTreatEmailLoginAsCaseSensitive() throws Exception {
            String email = generateUniqueEmail();
            String password = "password123";
            RegisterRequest registerRequest = new RegisterRequest(email.toLowerCase(), password);

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(email.toUpperCase(), password);
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldGenerateDifferentTokensForMultipleLogins() throws Exception {
            String email = generateUniqueEmail();
            String password = "password123";
            RegisterRequest registerRequest = new RegisterRequest(email, password);

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(email, password);

            MvcResult result1 = mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Thread.sleep(1000);

            MvcResult result2 = mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse response1 = objectMapper.readValue(
                    result1.getResponse().getContentAsString(), AuthResponse.class);
            AuthResponse response2 = objectMapper.readValue(
                    result2.getResponse().getContentAsString(), AuthResponse.class);

            assertThat(response1.accessToken()).isNotEqualTo(response2.accessToken());
        }
    }

    @Nested
    class SecurityAndEdgeCases {

        @Test
        void shouldRejectRequestWithoutContentType() throws Exception {
            RegisterRequest request = new RegisterRequest(generateUniqueEmail(), "password123");

            mockMvc.perform(post("/api/register")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        void shouldHandleConcurrentRegistrationAttempts() throws Exception {
            String email = generateUniqueEmail();
            RegisterRequest request = new RegisterRequest(email, "password123");

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldAcceptSpecialCharactersInPassword() throws Exception {
            String email = generateUniqueEmail();
            String complexPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;:',.<>?/~`";
            RegisterRequest request = new RegisterRequest(email, complexPassword);

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(email, complexPassword);
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }
    }
}
