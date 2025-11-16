package com.macode101.shortenerurl.controller;

import com.macode101.shortenerurl.dto.ShortenUrlRequest;
import com.macode101.shortenerurl.dto.ShortenUrlResponse;
import com.macode101.shortenerurl.dto.UrlListResponse;
import static com.macode101.shortenerurl.security.AuthorizeConstants.ADMIN;
import static com.macode101.shortenerurl.security.AuthorizeConstants.USER;
import com.macode101.shortenerurl.service.UrlService;
import com.macode101.shortenerurl.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UrlController {
    
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    @PreAuthorize("hasAnyAuthority('" + USER + "', '" + ADMIN + "')")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        String userId = SecurityUtils.getCurrentUserLogin();
        ShortenUrlResponse response = urlService.createShortUrl(request.originalUrl(), userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/urls")
    @PreAuthorize("hasAnyAuthority('" + USER + "', '" + ADMIN + "')")
    public ResponseEntity<List<UrlListResponse>> getUserUrls() {
        String userId = SecurityUtils.getCurrentUserLogin();
        List<UrlListResponse> urls = urlService.getUserUrls(userId);
        
        return ResponseEntity.ok(urls);
    }

    @DeleteMapping("/urls/{id}")
    @PreAuthorize("hasAnyAuthority('" + USER + "', '" + ADMIN + "')")
    public ResponseEntity<Void> deleteUrl(@Valid @PathVariable Long id) {
        String userId = SecurityUtils.getCurrentUserLogin();
        urlService.deleteUrl(id, userId);
        return ResponseEntity.noContent().build();
    }
}
