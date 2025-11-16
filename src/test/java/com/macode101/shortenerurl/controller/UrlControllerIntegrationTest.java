package com.macode101.shortenerurl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macode101.shortenerurl.dto.AuthResponse;
import com.macode101.shortenerurl.dto.RegisterRequest;
import com.macode101.shortenerurl.dto.ShortenUrlRequest;
import com.macode101.shortenerurl.dto.ShortenUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userEmail;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        userEmail = generateUniqueEmail();
        authToken = registerAndGetToken(userEmail, "password123");
    }

    private String generateUniqueEmail() {
        return "user-" + UUID.randomUUID() + "@macode101.com";
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, password);
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

    private String createSecondUser() throws Exception {
        return registerAndGetToken(generateUniqueEmail(), "password124");
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
    class CreateShortUrl {

        @Test
        @DisplayName("Should create short URL with valid input")
        void shouldCreateShortUrlWhenAuthenticated() throws Exception {
            ShortenUrlRequest request = new ShortenUrlRequest("https://www.macode101.com");

            MvcResult result = mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shortUrl").exists())
                    .andExpect(jsonPath("$.shortCode").exists())
                    .andExpect(jsonPath("$.originalUrl").value("https://www.macode101.com"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ShortenUrlResponse response = objectMapper.readValue(responseBody, ShortenUrlResponse.class);
            assertThat(response.shortCode()).hasSizeBetween(6,8);
            assertThat(response.shortUrl()).contains(response.shortCode());
        }

        @Test
        @DisplayName("Should accept URLs with different protocols")
        void shouldCreateShortUrlsWithDifferentProtocols() throws Exception {
            String[] validUrls = {
                    "https://www.macode101.com",
                    "http://www.macode101.com",
                    "https://macode101.com/path/to/resource",
                    "https://subdomain.macode101.com",
                    "https://macode101.com:8080/path"
            };

            for (String url : validUrls) {
                ShortenUrlRequest request = new ShortenUrlRequest(url);
                mockMvc.perform(post("/api/shorten")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.originalUrl").value(url));
            }
        }

        @Test
        void shouldGenerateUniqueShortCodesForSameUrl() throws Exception {
            String url = "https://www.macode101.com";
            ShortenUrlRequest request = new ShortenUrlRequest(url);

            MvcResult result1 = mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            ShortenUrlResponse response1 = objectMapper.readValue(
                    result1.getResponse().getContentAsString(), ShortenUrlResponse.class);
            ShortenUrlResponse response2 = objectMapper.readValue(
                    result2.getResponse().getContentAsString(), ShortenUrlResponse.class);

            assertThat(response1.shortCode()).isNotEqualTo(response2.shortCode());
        }

        @Test
        void shouldRejectShortenUrlWithoutAuthentication() throws Exception {
            ShortenUrlRequest request = new ShortenUrlRequest("https://www.macode101.com");

            mockMvc.perform(post("/api/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectInvalidBearerToken() throws Exception {
            ShortenUrlRequest request = new ShortenUrlRequest("https://www.macode101.com");

            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectInvalidUrlFormat() throws Exception {
            ShortenUrlRequest request = new ShortenUrlRequest("not-a-valid-url");

            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUrlWithoutProtocol() throws Exception {
            ShortenUrlRequest request = new ShortenUrlRequest("www.macode101.com");

            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectEmptyUrl() throws Exception {
            ShortenUrlRequest request = new ShortenUrlRequest("");

            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectNullUrl() throws Exception {
            String requestJson = "{\"originalUrl\":null}";

            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldHandleUrlWithQueryParameters() throws Exception {
            String urlWithParams = "https://www.macode101.com/search?q=test&page=1&sort=desc";
            ShortenUrlResponse response = createShortUrl(urlWithParams);

            assertThat(response.originalUrl()).isEqualTo(urlWithParams);
        }

        @Test
        void shouldHandleUrlWithFragments() throws Exception {
            String urlWithFragment = "https://www.macode101.com/page#section-1";
            ShortenUrlResponse response = createShortUrl(urlWithFragment);

            assertThat(response.originalUrl()).isEqualTo(urlWithFragment);
        }

        @Test
        void shouldHandleVeryLongUrls() throws Exception {
            StringBuilder longUrl = new StringBuilder("https://www.macode101.com/path?");
            for (int i = 0; i < 50; i++) {
                longUrl.append("param").append(i).append("=value").append(i).append("&");
            }

            ShortenUrlResponse response = createShortUrl(longUrl.toString());
            assertThat(response.shortCode()).hasSizeBetween(6,8);
        }
    }

    @Nested
    class ListUserUrls {

        @Test
        void shouldListUserUrls() throws Exception {
            createShortUrl("https://www.macode1011.com");
            createShortUrl("https://www.macode1012.com");

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].shortCode").exists())
                    .andExpect(jsonPath("$[0].shortUrl").exists())
                    .andExpect(jsonPath("$[0].originalUrl").exists())
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }

        @Test
        void shouldReturnEmptyArrayWhenNoUrls() throws Exception {
            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void shouldOnlyListOwnUrls() throws Exception {
            createShortUrl("https://www.user1-url.com");

            String secondUserToken = createSecondUser();
            ShortenUrlRequest request = new ShortenUrlRequest("https://www.user2-url.com");
            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + secondUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].originalUrl").value("https://www.user1-url.com"));
        }

        @Test
        void shouldRejectListUrlsWithoutAuthentication() throws Exception {
            mockMvc.perform(get("/api/urls"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldOrderUrlsByCreationDateDescending() throws Exception {
            createShortUrl("https://www.first.com");
            createShortUrl("https://www.second.com");
            createShortUrl("https://www.third.com");

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].originalUrl").value("https://www.third.com"))
                    .andExpect(jsonPath("$[1].originalUrl").value("https://www.second.com"))
                    .andExpect(jsonPath("$[2].originalUrl").value("https://www.first.com"));
        }

        @Test
        void shouldIncludeBothActiveAndInactiveUrls() throws Exception {
            createShortUrl("https://www.active.com");
            createShortUrl("https://www.to-delete.com");
            Long urlId = getFirstUrlId();

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[*].active", containsInAnyOrder(true, false)));
        }
    }

    @Nested
    class DeleteUrl {

        @Test
        void shouldDeleteUrl() throws Exception {
            createShortUrl("https://www.macode101.com");
            Long urlId = getFirstUrlId();

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].active").value(false));
        }

        @Test
        void shouldRejectDeleteWithoutAuthentication() throws Exception {
            mockMvc.perform(delete("/api/urls/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectDeleteWithInvalidToken() throws Exception {
            createShortUrl("https://www.macode101.com");
            Long urlId = getFirstUrlId();

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectDeleteOfNonExistentUrl() throws Exception {
            mockMvc.perform(delete("/api/urls/99999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectDeleteOfAnotherUsersUrl() throws Exception {
            createShortUrl("https://www.macode101.com");
            Long urlId = getFirstUrlId();

            String secondUserToken = createSecondUser();

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + secondUserToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldHandleDeletingAlreadyDeletedUrl() throws Exception {
            createShortUrl("https://www.macode101.com");
            Long urlId = getFirstUrlId();

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        void shouldRejectInvalidUrlIdFormat() throws Exception {
            mockMvc.perform(delete("/api/urls/invalid")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class IntegrationScenarios {

        @Test
        void shouldHandleCompleteUrlLifecycle() throws Exception {
            ShortenUrlResponse response = createShortUrl("https://www.macode101.com");
            assertThat(response.shortCode()).isNotNull();

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].active").value(true));

            Long urlId = getFirstUrlId();
            mockMvc.perform(delete("/api/urls/" + urlId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].active").value(false));
        }

        @Test
        void shouldHandleMultipleUsersIndependently() throws Exception {
            createShortUrl("https://www.user1.com");

            String secondUserToken = createSecondUser();
            ShortenUrlRequest request = new ShortenUrlRequest("https://www.user2.com");
            mockMvc.perform(post("/api/shorten")
                            .header("Authorization", "Bearer " + secondUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].originalUrl").value("https://www.user1.com"));

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + secondUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].originalUrl").value("https://www.user2.com"));
        }

        @Test
        void shouldHandleCreatingManyUrls() throws Exception {
            int urlCount = 10;
            for (int i = 0; i < urlCount; i++) {
                createShortUrl("https://www.macode101" + i + ".com");
            }

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(urlCount));
        }

        @Test
        void shouldHandleBulkUrlCreationWithUniqueShortCodes() throws Exception {
            int urlCount = 20;
            Set<String> shortCodes = new HashSet<>();

            for (int i = 0; i < urlCount; i++) {
                ShortenUrlResponse response = createShortUrl("https://www.macode101-" + i + ".com");
                shortCodes.add(response.shortCode());
            }

            assertThat(shortCodes).hasSize(urlCount);
        }
    }

    @Nested
    class PerformanceTests {

        @Test
        void shouldHandleRapidSequentialUrlCreation() throws Exception {
            long startTime = System.currentTimeMillis();
            int urlCount = 50;

            for (int i = 0; i < urlCount; i++) {
                createShortUrl("https://www.macode101-test-" + i + ".com");
            }

            long duration = System.currentTimeMillis() - startTime;

            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(urlCount));

            assertThat(duration).isLessThan(10000);
        }

        @Test
        void shouldMaintainPerformanceWithLargeDataset() throws Exception {
            for (int i = 0; i < 100; i++) {
                createShortUrl("https://www.dataset-" + i + ".com");
            }

            long startTime = System.currentTimeMillis();
            mockMvc.perform(get("/api/urls")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(100));
            long duration = System.currentTimeMillis() - startTime;

            assertThat(duration).isLessThan(2000);
        }
    }
}
