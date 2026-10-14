package com.college.booking.security;

import com.college.booking.entity.User;
import com.college.booking.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserDetails currentDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw ApiException.unauthorized("Authentication required.");
        }
        return details;
    }

    public static User currentUser() {
        return currentDetails().getUser();
    }

    public static Long currentUserId() {
        return currentDetails().getId();
    }
}
