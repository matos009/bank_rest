package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @Size(min = 6, max = 255) @NotBlank String newPassword
) {}
