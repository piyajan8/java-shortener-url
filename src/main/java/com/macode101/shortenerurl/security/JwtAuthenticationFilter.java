package com.macode101.shortenerurl.security;

import com.macode101.shortenerurl.dto.JwtPrincipal;
import com.macode101.shortenerurl.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SecurityUrlPermit securityUrlPermit;
    private final JwtAuthenticationFailureHandler failureHandler;

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, SecurityUrlPermit securityUrlPermit, JwtAuthenticationFailureHandler failureHandler) {
        this.jwtUtil = jwtUtil;
        this.securityUrlPermit = securityUrlPermit;
        this.failureHandler = failureHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return securityUrlPermit.urls().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");

        String accessToken = getAuthorizationHeader(authorizationHeader);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtUtil.validateToken(accessToken)) {

                    String userId = jwtUtil.extractClaim(accessToken, claims -> claims.get("userId", String.class));
                    String email = jwtUtil.extractClaim(accessToken, claims -> claims.get("email", String.class));
                    List<String> roles = jwtUtil.extractClaim(accessToken, claims -> {
                        Object raw = claims.get("roles");
                        return raw instanceof List<?> list
                                ? list.stream().map(String::valueOf).toList()
                                : List.of();
                    });

                    List<GrantedAuthority> authorities = roles.stream()
                            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                            .toList();

                    JwtPrincipal principal = new JwtPrincipal(userId, email);

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception ex) {
                failureHandler.onAuthenticationFailure(request, response, ex);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String getAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
