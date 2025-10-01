package com.example.bankcards.security.utils;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static Long currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return null;

        Object p = a.getPrincipal();
        if (p instanceof Long l) return l;
        if (p instanceof String s) return parseOrNull(s);
        if (p instanceof UserDetails ud) return parseOrNull(ud.getUsername());
        return null;
    }

    private static Long parseOrNull(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return null; }
    }
}