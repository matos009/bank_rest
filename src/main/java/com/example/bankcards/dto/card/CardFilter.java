package com.example.bankcards.dto.card;
import com.example.bankcards.entity.enums.CardStatus;
import java.time.OffsetDateTime;

public record CardFilter(
        CardStatus status,                 // nullable
        OffsetDateTime createdFrom,        // nullable
        OffsetDateTime createdTo           // nullable
) {}