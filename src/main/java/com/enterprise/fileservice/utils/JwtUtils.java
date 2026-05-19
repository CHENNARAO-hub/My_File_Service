package com.enterprise.fileservice.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    // Extracts preferred_username from Keycloak JWT
    // preferred_username is what user typed at Keycloak login
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtToken) {
            String username = (String) jwtToken
                                .getTokenAttributes()
                                .get("preferred_username");
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        // fallback — getName() returns subject claim
        return auth.getName();
    }

    // Get any specific claim from JWT
    public String getClaim(String claimName) {
        Authentication auth = SecurityContextHolder
                                .getContext()
                                .getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtToken) {
            Object value = jwtToken.getTokenAttributes().get(claimName);
            return value != null ? value.toString() : null;
        }
        return null;
    }
}