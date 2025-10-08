package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.*;

public interface CardService {


    CardResponse createForOwner(Long ownerId, CreateCardRequest req);


    Page<CardResponse> findMyCards(Long userId, CardStatus status, Pageable pageable);


    Page<CardResponse> adminList(CardFilter filter, Pageable pageable);


    CardResponse getMine(Long userId, Long cardId);


    void changeStatus(Long cardId, CardStatus newStatus);


    CardResponse updateExpiry(Long cardId, UpdateExpiryRequest req);


    void delete(Long cardId);
}