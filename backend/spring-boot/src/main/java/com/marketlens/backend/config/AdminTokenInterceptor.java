package com.marketlens.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketlens.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminTokenInterceptor implements HandlerInterceptor {
    private final String adminToken;
    private final ObjectMapper objectMapper;

    public AdminTokenInterceptor(
            @Value("${market-lens.admin-token:}") String adminToken,
            ObjectMapper objectMapper
    ) {
        this.adminToken = adminToken;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String suppliedToken = request.getHeader("X-Admin-Token");
        if (adminToken.isBlank() || !matches(adminToken, suppliedToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(401, "Unauthorized admin request"));
            return false;
        }
        return true;
    }

    private boolean matches(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8)
        );
    }
}
