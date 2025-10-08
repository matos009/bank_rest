package com.example.bankcards.jwt;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
class JwtFilterTestControllers {

    @GetMapping("/open/ping")
    String ping() {
        return "pong";
    }

    @GetMapping("/secure/me")
    String me(Authentication authentication) {
        Object p = authentication.getPrincipal();
        return "userId=" + p;
    }

    @GetMapping("/secure/admin")
    String adminOnly() {
        return "ok";
    }
}