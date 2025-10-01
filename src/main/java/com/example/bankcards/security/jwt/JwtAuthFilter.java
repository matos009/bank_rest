package com.example.bankcards.security.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(h) && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            if (jwt.validate(token)) {
                Long userId = jwt.getUserId(token).orElse(null);
                List<String> roles = jwt.getRoles(token);
                if (userId != null) {
                    var authorities = roles.stream()
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    var auth = new JwtAuthentication(userId, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(req, res);
    }

    static class JwtAuthentication extends AbstractAuthenticationToken {
        private final Long userId;

        JwtAuthentication(Long userId, List<SimpleGrantedAuthority> authorities) {
            super(authorities);
            this.userId = userId;
            setAuthenticated(true);
        }

        @Override public Object getCredentials() { return ""; }
        @Override public Object getPrincipal() { return userId; }
    }
}