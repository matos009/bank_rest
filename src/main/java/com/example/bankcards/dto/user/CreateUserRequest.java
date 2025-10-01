package com.example.bankcards.dto.user;

import jakarta.validation.constraints.*;
import java.util.Set;

public record CreateUserRequest(
        @Email @NotBlank String email,
        @Size(min = 6, max = 255) @NotBlank String password,
        @Size(max = 255) String fullName,

        Set<String> roles
) {}