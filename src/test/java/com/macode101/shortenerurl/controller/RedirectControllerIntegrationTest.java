package com.macode101.shortenerurl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macode101.shortenerurl.dto.AuthResponse;
import com.macode101.shortenerurl.dto.RegisterRequest;
import com.macode101.shortenerurl.dto.ShortenUrlRequest;
import com.macode101.shortenerurl.dto.ShortenUrlResponse;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedirectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private String shortCode;
    private final String originalUrl = "https://www.macode101.com";

    @BeforeEach
    void setUp() throws Exception {
        String userEmail = generateUniqueEmail();
        authToken = registerAndGetToken(userEmail);
        shortCode = createShortUrl(originalUrl).shortCode();
    }

    private String generateUniqueEmail() {
        return "user-" + UUID.randomUUID() + "@macode101.com";
    }

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, "password123");
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        return authResponse.accessToken();
    }

    private ShortenUrlResponse createShortUrl(String url) throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest(url);
        MvcResult result = mockMvc.perform(post("/api/shorten")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, ShortenUrlResponse.class);
    }

    private Long getFirstUrlId() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/urls")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        String listBody = listResult.getResponse().getContentAsString();
        return objectMapper.readTree(listBody).get(0).get("id").asLong();
    }

    @Nested
    class RedirectToOriginalUrl {

        @Test
        void shouldRedirectToOriginalUrl() throws Exception {
            mockMvc.perform(get("/r/" + shortCode))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", originalUrl))
                    .andExpect(header().exists("Location"));
        }

        @Test
        void shouldRedirectWithoutAuthentication() throws Exception {
            mockMvc.perform(get("/r/" + shortCode))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", originalUrl));
        }

        @Test
        void shouldHandleMultipleRedirects() throws Exception {
            String url1 = "https://www.macode1011.com";
            String url2 = "https://www.macode1012.com";
            String url3 = "https://www.macode1013.com/path?query=value";

            ShortenUrlResponse response1 = createShortUrl(url1);
            ShortenUrlResponse response2 = createShortUrl(url2);
            ShortenUrlResponse response3 = createShortUrl(url3);

            mockMvc.perform(get("/r/" + response1.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", url1));

            mockMvc.perform(get("/r/" + response2.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", url2));

            mockMvc.perform(get("/r/" + response3.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", url3));
        }

        @Test
        void shouldHandleRepeatedRedirects() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/r/" + shortCode))
                        .andExpect(status().isFound())
                        .andExpect(header().string("Location", originalUrl));
            }
        }

        @Test
        void shouldRedirectToUrlWithSpecialCharacters() throws Exception {
            String specialUrl = "https://www.macode101.com/path?name=John%20Doe&tags=java,spring,test";
            ShortenUrlResponse response = createShortUrl(specialUrl);

            mockMvc.perform(get("/r/" + response.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", specialUrl));
        }

        @Test
        void shouldRedirectToUrlWithFragment() throws Exception {
            String urlWithFragment = "https://www.macode101.com/page#section-2";
            ShortenUrlResponse response = createShortUrl(urlWithFragment);

            mockMvc.perform(get("/r/" + response.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", urlWithFragment));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldReturnNotFoundForInvalidShortCode() throws Exception {
            mockMvc.perform(get("/r/invalid"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundForNonExistentShortCode() throws Exception {
            mockMvc.perform(get("/r/abcd1234"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnErrorForEmptyShortCode() throws Exception {
            mockMvc.perform(get("/r/"))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        void shouldReturnGoneForDeactivatedUrl() throws Exception {
            Long urlId = getFirstUrlId();

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/r/" + shortCode))
                    .andExpect(status().isGone());
        }

        @Test
        void shouldReturnNotFoundForShortCodeWithSpecialCharacters() throws Exception {
            mockMvc.perform(get("/r/abc@123"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundForVeryLongShortCode() throws Exception {
            String longCode = "a".repeat(100);
            mockMvc.perform(get("/r/" + longCode))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldHandleCaseSensitivityInShortCodes() throws Exception {
            String upperCaseCode = shortCode.toUpperCase();

            if (!shortCode.equals(upperCaseCode)) {
                mockMvc.perform(get("/r/" + upperCaseCode))
                        .andExpect(status().isNotFound());
            }
        }
    }

    @Nested
    class IntegrationScenarios {

        @Test
        void shouldHandleCompleteLifecycle() throws Exception {
            String testUrl = "https://www.lifecycle-test.com";
            ShortenUrlResponse response = createShortUrl(testUrl);
            String testShortCode = response.shortCode();

            mockMvc.perform(get("/r/" + testShortCode))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", testUrl));

            Long urlId = getFirstUrlId();
            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/r/" + testShortCode))
                    .andExpect(status().isGone());
        }

        @Test
        void shouldHandleMultipleUsersWithDifferentShortUrls() throws Exception {
            String user1Url = "https://www.user1.com";
            ShortenUrlResponse response1 = createShortUrl(user1Url);

            String user2Token = registerAndGetToken(generateUniqueEmail());
            String user2Url = "https://www.user2.com";
            ShortenUrlRequest request2 = new ShortenUrlRequest(user2Url);
            MvcResult result2 = mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String body2 = result2.getResponse().getContentAsString();
            ShortenUrlResponse response2 = objectMapper.readValue(body2, ShortenUrlResponse.class);

            mockMvc.perform(get("/r/" + response1.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", user1Url));

            mockMvc.perform(get("/r/" + response2.shortCode()))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", user2Url));

            assertThat(response1.shortCode()).isNotEqualTo(response2.shortCode());
        }

        @Test
        void shouldHandleHighVolumeOfRedirects() throws Exception {
            int redirectCount = 100;

            for (int i = 0; i < redirectCount; i++) {
                mockMvc.perform(get("/r/" + shortCode))
                        .andExpect(status().isFound())
                        .andExpect(header().string("Location", originalUrl));
            }
        }
    }
}
