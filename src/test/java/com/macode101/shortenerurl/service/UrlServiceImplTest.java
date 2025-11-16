package com.macode101.shortenerurl.service;

import com.macode101.shortenerurl.config.ApplicationConfiguration;
import com.macode101.shortenerurl.dto.ShortenUrlResponse;
import com.macode101.shortenerurl.dto.UrlListResponse;
import com.macode101.shortenerurl.entity.ShortenedUrl;
import com.macode101.shortenerurl.exception.BadRequestException;
import com.macode101.shortenerurl.exception.ForbiddenException;
import com.macode101.shortenerurl.exception.ResourceNotFoundException;
import com.macode101.shortenerurl.exception.UrlShortenerException;
import com.macode101.shortenerurl.repository.ShortenedUrlRepository;
import com.macode101.shortenerurl.repository.UserRepository;
import com.macode101.shortenerurl.util.ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private ShortenedUrlRepository shortenedUrlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UrlServiceImpl urlService;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String USER_ID = "userId";

    @Test
    void createShortUrlWithValidUrlShouldReturnShortenUrlResponse() {
        String originalUrl = "https://example.com/very/long/url";
        String shortCode = "abc123";

        when(applicationConfiguration.getBaseUrl()).thenReturn(BASE_URL);
        when(userRepository.existsByUid(USER_ID)).thenReturn(true);
        when(shortCodeGenerator.generate()).thenReturn(shortCode);
        when(shortenedUrlRepository.existsByShortCode(shortCode)).thenReturn(false);
        when(shortenedUrlRepository.save(any(ShortenedUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortenUrlResponse response = urlService.createShortUrl(originalUrl, USER_ID);

        assertNotNull(response);
        assertEquals(BASE_URL + "/r/" + shortCode, response.shortUrl());
        assertEquals(shortCode, response.shortCode());
        assertEquals(originalUrl, response.originalUrl());

        verify(shortCodeGenerator).generate();
        verify(shortenedUrlRepository).save(any(ShortenedUrl.class));
    }

    @Test
    void createShortUrlWithNonExistentUserShouldThrowResourceNotFoundException() {
        String originalUrl = "https://example.com";
        when(userRepository.existsByUid(USER_ID)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> urlService.createShortUrl(originalUrl, USER_ID));
    }

    @Test
    void createShortUrlWithCollisionShouldRetryAndSucceed() {
        String originalUrl = "https://example.com";
        String firstCode = "abc123";
        String secondCode = "def456";

        when(applicationConfiguration.getBaseUrl()).thenReturn(BASE_URL);
        when(userRepository.existsByUid(USER_ID)).thenReturn(true);
        when(shortCodeGenerator.generate()).thenReturn(firstCode, secondCode);
        when(shortenedUrlRepository.existsByShortCode(firstCode)).thenReturn(true);
        when(shortenedUrlRepository.existsByShortCode(secondCode)).thenReturn(false);
        when(shortenedUrlRepository.save(any(ShortenedUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortenUrlResponse response = urlService.createShortUrl(originalUrl, USER_ID);

        assertNotNull(response);
        assertEquals(secondCode, response.shortCode());
        verify(shortCodeGenerator, times(2)).generate();
        verify(shortenedUrlRepository).existsByShortCode(firstCode);
        verify(shortenedUrlRepository).existsByShortCode(secondCode);
    }

    @Test
    void createShortUrlWithMaxCollisionsShouldThrowUrlShortenerException() {
        String originalUrl = "https://example.com";
        
        when(userRepository.existsByUid(USER_ID)).thenReturn(true);
        when(shortCodeGenerator.generate()).thenReturn("code1", "code2", "code3", "code4", "code5");
        when(shortenedUrlRepository.existsByShortCode(anyString())).thenReturn(true);

        assertThrows(UrlShortenerException.class, () -> urlService.createShortUrl(originalUrl, USER_ID));
        verify(shortCodeGenerator, times(5)).generate();
    }

    @Test
    void getUserUrlsWithValidUserShouldReturnUrlList() {
        ShortenedUrl url1 = createShortenedUrl(1L, "abc123", "https://example.com/1", USER_ID, true);
        ShortenedUrl url2 = createShortenedUrl(2L, "def456", "https://example.com/2", USER_ID, false);
        
        when(shortenedUrlRepository.findByUidOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Arrays.asList(url1, url2));

        List<UrlListResponse> responses = urlService.getUserUrls(USER_ID);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("abc123", responses.get(0).shortCode());
        assertEquals("def456", responses.get(1).shortCode());
        assertEquals(true, responses.get(0).active());
        assertEquals(false, responses.get(1).active());
        verify(shortenedUrlRepository).findByUidOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void deleteUrlWithValidOwnershipShouldDeactivateUrl() {
        Long urlId = 1L;
        ShortenedUrl url = createShortenedUrl(urlId, "abc123", "https://example.com", USER_ID, true);
        
        when(shortenedUrlRepository.findById(urlId)).thenReturn(Optional.of(url));
        when(shortenedUrlRepository.save(any(ShortenedUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        urlService.deleteUrl(urlId, USER_ID);

        assertFalse(url.getActive());
        verify(shortenedUrlRepository).findById(urlId);
        verify(shortenedUrlRepository).save(url);
    }

    @Test
    void deleteUrlWithInvalidOwnershipShouldThrowForbiddenException() {
        Long urlId = 1L;
        String differentUserId = "different-user-uid";
        ShortenedUrl url = createShortenedUrl(urlId, "abc123", "https://example.com", differentUserId, true);
        
        when(shortenedUrlRepository.findById(urlId)).thenReturn(Optional.of(url));

        assertThrows(ForbiddenException.class, () -> urlService.deleteUrl(urlId, USER_ID));
        verify(shortenedUrlRepository).findById(urlId);
        verify(shortenedUrlRepository, never()).save(any(ShortenedUrl.class));
    }

    @Test
    void deleteUrlWithNonExistentUrlShouldThrowResourceNotFoundException() {
        Long urlId = 999L;
        when(shortenedUrlRepository.findById(urlId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> urlService.deleteUrl(urlId, USER_ID));
        verify(shortenedUrlRepository).findById(urlId);
    }

    @Test
    void getOriginalUrlWithValidShortCodeShouldReturnOriginalUrl() {
        String shortCode = "abc123";
        String originalUrl = "https://example.com";
        ShortenedUrl url = createShortenedUrl(1L, shortCode, originalUrl, USER_ID, true);
        
        when(shortenedUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        String result = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, result);
        verify(shortenedUrlRepository).findByShortCode(shortCode);
    }

    @Test
    void getOriginalUrlWithNonExistentShortCodeShouldThrowResourceNotFoundException() {
        String shortCode = "invalid";
        when(shortenedUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> urlService.getOriginalUrl(shortCode));
        verify(shortenedUrlRepository).findByShortCode(shortCode);
    }

    @Test
    void getOriginalUrlWithDeactivatedUrlShouldThrowBadRequestException() {
        String shortCode = "abc123";
        ShortenedUrl url = createShortenedUrl(1L, shortCode, "https://example.com", USER_ID, false);
        
        when(shortenedUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        assertThrows(BadRequestException.class, () -> urlService.getOriginalUrl(shortCode));
        verify(shortenedUrlRepository).findByShortCode(shortCode);
    }

    private ShortenedUrl createShortenedUrl(Long id, String shortCode, String originalUrl, String uid, boolean active) {
        ShortenedUrl url = new ShortenedUrl();
        url.setId(id);
        url.setShortCode(shortCode);
        url.setOriginalUrl(originalUrl);
        url.setUid(uid);
        url.setActive(active);
        url.setCreatedAt(LocalDateTime.now());
        return url;
    }
}
