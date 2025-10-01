package com.example.bankcards.controller;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.security.utils.SecurityUtils;
import com.example.bankcards.service.card.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "Cards")
@RestController
@RequestMapping("/cards")
@Validated
public class CardController {

    private final CardService cards;

    public CardController(CardService cards) {
        this.cards = cards;
    }

    // ---------- ADMIN: создать карту конкретному пользователю ----------
    @Operation(summary = "Create card for owner (ADMIN)")
    @PostMapping("/admin/owner/{ownerId}")
    public CardResponse createForOwner(@PathVariable Long ownerId,
                                       @RequestBody @Valid CreateCardRequest req) {
        return cards.createForOwner(ownerId, req);
    }

    // ---------- ADMIN: список карт с фильтрами + пагинацией ----------
    @Operation(summary = "Admin list cards with filters (ADMIN)")
    @GetMapping("/admin")
    public Page<CardResponse> adminList(@ParameterObject @ModelAttribute CardFilter filter,
                                        @ParameterObject Pageable pageable) {
        return cards.adminList(filter, pageable);
    }

    // ---------- USER: мои карты (пока userId из query — до JWT) ----------
    @Operation(summary = "List my cards (temporary userId param, till JWT)")
    @GetMapping("/me")
    public Page<CardResponse> myCards(@RequestParam(value = "status", required = false) CardStatus status,
                                      @ParameterObject Pageable pageable) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException("Unauthenticated");
        }
        return cards.findMyCards(userId, status, pageable);
    }

    // ---------- ADMIN: сменить статус карты ----------
    @Operation(summary = "Change card status (ADMIN)")
    @PatchMapping("/{id}/status")
    public void changeStatus(@PathVariable Long id,
                             @RequestBody @Valid ChangeStatusRequest req) {
        cards.changeStatus(id, req.status());
    }

    // ---------- ADMIN: обновить срок действия карты ----------
    @Operation(summary = "Update card expiry (ADMIN)")
    @PatchMapping("/{id}/expiry")
    public CardResponse updateExpiry(@PathVariable Long id,
                                     @RequestBody @Valid UpdateExpiryRequest req) {
        return cards.updateExpiry(id, req);
    }

    // ---------- ADMIN: удалить карту ----------
    @Operation(summary = "Delete card (ADMIN)")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cards.delete(id);
    }

}