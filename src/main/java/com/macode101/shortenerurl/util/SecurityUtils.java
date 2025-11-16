package com.macode101.shortenerurl.util;

import com.macode101.shortenerurl.dto.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return extractPrincipal(securityContext.getAuthentication()).userId();
    }

    private static JwtPrincipal extractPrincipal(Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof JwtPrincipal jwtPrincipal) {
                return jwtPrincipal;
            }
        }

        return null;
    }
}
