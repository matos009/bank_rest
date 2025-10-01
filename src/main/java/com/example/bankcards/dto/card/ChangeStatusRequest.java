package com.example.bankcards.dto.card;

import com.example.bankcards.entity.enums.CardStatus;

public record ChangeStatusRequest(CardStatus status) {}