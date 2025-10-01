package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record SetRolesRequest(
        @NotEmpty Set<String> roles
) {}

