package com.example.bankcards.dto.transfer;

import com.example.bankcards.entity.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;


public record TransferResponse(
        Long id,
        Long userId,
        Long fromCardId,
        Long toCardId,
        BigDecimal amount,
        TransferStatus status,
        String description,
        OffsetDateTime createdAt
) {}