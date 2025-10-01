package com.example.bankcards.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 255) String fullName,
        Boolean enabled
) {}

