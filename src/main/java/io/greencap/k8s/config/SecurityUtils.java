package io.greencap.k8s.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static boolean isViewer() {
        return hasRole("ROLE_VIEWER");
    }

    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    private static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
