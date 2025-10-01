package com.example.bankcards.util;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Transfer;

public final class TransferMapper {
    private TransferMapper() {}

    public static TransferResponse toDto(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getUser().getId(),
                t.getFromCard().getId(),
                t.getToCard().getId(),
                t.getAmount(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}