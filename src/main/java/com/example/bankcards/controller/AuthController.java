package com.example.bankcards.controller;

import com.example.bankcards.security.jwt.JwtService;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final UserService users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserService users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public static record LoginRequest(@Email String email, @NotBlank String password) {}
    public static record LoginResponse(String token, Instant expiresAt) {}

    @Operation(summary = "Login with email/password → JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        User user = users.getEntityByEmailOrThrow(req.email());
        if (!user.isEnabled()) {
            throw new BusinessException("User disabled");
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        List<String> roles = user.getRoles().stream().map(r -> r.getName()).toList();
        String token = jwt.generateToken(user.getId(), user.getEmail(), roles);
        return ResponseEntity.ok(new LoginResponse(token, jwt.getExpiry(token)));
    }
}