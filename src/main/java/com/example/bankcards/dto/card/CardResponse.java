package com.example.bankcards.dto.card;

import com.example.bankcards.entity.enums.CardStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CardResponse(
        Long id,
        Long ownerId,
        String maskedPan,
        short expMonth,
        short expYear,
        CardStatus status,
        BigDecimal balance,
        OffsetDateTime createdAt
) {}