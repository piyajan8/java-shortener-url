package com.macode101.shortenerurl.dto;

import java.time.LocalDateTime;

public record UrlListResponse(
    Long id,
    String shortCode,
    String shortUrl,
    String originalUrl,
    Boolean active,
    LocalDateTime createdAt
) {}
