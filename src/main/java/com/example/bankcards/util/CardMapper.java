package com.example.bankcards.util;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;

public class CardMapper {
    private CardMapper() {}

    public static CardResponse toDto(Card c) {
        String masked = mask(c.getPanLast4());
        return new CardResponse(
                c.getId(),
                c.getUser().getId(),
                masked,
                c.getExpMonth(),
                c.getExpYear(),
                c.getStatus(),
                c.getBalance(),
                c.getCreatedAt()
        );
    }

    public static String mask(String last4) {
        return "**** **** **** " + last4;
    }
}