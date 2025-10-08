package com.example.bankcards.dto.login;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt) {}