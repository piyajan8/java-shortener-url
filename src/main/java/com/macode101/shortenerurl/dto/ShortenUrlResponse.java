package com.macode101.shortenerurl.dto;

public record ShortenUrlResponse(
    String shortUrl,
    String shortCode,
    String originalUrl
) {}
