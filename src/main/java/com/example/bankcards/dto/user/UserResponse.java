
package com.example.bankcards.dto.user;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        boolean enabled,
        Set<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}