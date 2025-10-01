package com.example.bankcards.security.jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JwtService {

    String generateToken(long userId, String email, List<String> roles);

    boolean validate(String token);

    Optional<Long> getUserId(String token);

    Optional<String> getEmail(String token);

    List<String> getRoles(String token);

    Instant getExpiry(String token);
}