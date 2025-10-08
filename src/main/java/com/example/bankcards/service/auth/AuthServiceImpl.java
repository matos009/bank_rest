package com.example.bankcards.service.auth;


import com.example.bankcards.dto.login.LoginRequest;
import com.example.bankcards.dto.login.LoginResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.security.jwt.JwtService;
import com.example.bankcards.service.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthServiceImpl(UserService users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        User user = users.getEntityByEmailOrThrow(req.email());
        if (!user.isEnabled()) {
            throw new BusinessException("User disabled");
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        List<String> roles = user.getRoles().stream().map(r -> r.getName()).toList();
        String token = jwt.generateToken(user.getId(), user.getEmail(), roles);
        return new LoginResponse(token, jwt.getExpiry(token));
    }
}