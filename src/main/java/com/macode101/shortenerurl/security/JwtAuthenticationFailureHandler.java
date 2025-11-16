package com.macode101.shortenerurl.security;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFailureHandler {

    private final Gson gson = new Gson();

    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception
    ) throws IOException {

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", "UNAUTHORIZED");
        payload.put("message", exception.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(gson.toJson(payload));
        }
    }
}

