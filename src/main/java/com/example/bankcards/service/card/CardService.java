package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.*;

public interface CardService {

    // Админ создаёт карту пользователю
    CardResponse createForOwner(Long ownerId, CreateCardRequest req);

    // «Мои карты» (для конкретного пользователя)
    Page<CardResponse> findMyCards(Long userId, CardStatus status, Pageable pageable);

    // Админский список всех карт
    Page<CardResponse> adminList(CardFilter filter, Pageable pageable);

    // Получить одну карту (проверка владельца)
    CardResponse getMine(Long userId, Long cardId);

    // Сменить статус (админ)
    void changeStatus(Long cardId, CardStatus newStatus);

    // Обновить срок действия (админ)
    CardResponse updateExpiry(Long cardId, UpdateExpiryRequest req);

    // Удалить карту (админ или бизнес-логикой) — мягко/жёстко, у нас жёстко
    void delete(Long cardId);
}