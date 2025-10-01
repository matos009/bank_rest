package com.example.bankcards.dto.card;

import jakarta.validation.constraints.*;

public record CreateCardRequest(
        @Pattern(regexp = "\\d{12,19}", message = "PAN must be 12–19 digits")
        String pan,

        @Min(value = 1) @Max(value = 12)
        short expMonth,

        @Min(value = 2000) @Max(value = 2100)
        short expYear
) {}