package io.greencap.k8s.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private static final String ADMIN_USERNAME = "admin";

    private SecurityUtils() {}

    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return ADMIN_USERNAME.equals(authentication.getName());
    }
}
