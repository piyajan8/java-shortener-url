package com.macode101.shortenerurl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortenUrlRequest(
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    @Size(max = 2048, message = "URL exceeds maximum length of 2048 characters")
    String originalUrl
) {}
