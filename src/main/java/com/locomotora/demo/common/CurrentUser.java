package com.locomotora.demo.common;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    public static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private CurrentUser() {
    }

    public static UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return UUID.fromString(authentication.getPrincipal().toString());
    }

    /** Rol ADMIN en la base (lo pone el filtro JWT con el rol actual de la cuenta). */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

    /** Para los servicios del panel de admin (ademas de la regla de SecurityConfig). */
    public static UUID requireAdmin() {
        UUID id = id();
        if (!isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access is required");
        }
        return id;
    }
}
