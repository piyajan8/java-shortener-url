package com.macode101.shortenerurl.security;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SecurityUrlPermit {

    public List<String> urls() {
        return List.of(
                "/api/register",
                "/api/login",
                "/r/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**"
        );
    }
}
