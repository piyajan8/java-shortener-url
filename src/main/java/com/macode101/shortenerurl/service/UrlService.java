package com.macode101.shortenerurl.service;

import com.macode101.shortenerurl.dto.ShortenUrlResponse;
import com.macode101.shortenerurl.dto.UrlListResponse;

import java.util.List;

public interface UrlService {

    ShortenUrlResponse createShortUrl(String originalUrl, String userId);

    List<UrlListResponse> getUserUrls(String userId);

    void deleteUrl(Long id, String userId);

    String getOriginalUrl(String shortCode);
}
