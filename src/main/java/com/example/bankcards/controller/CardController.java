// src/main/java/com/example/bankcards/controller/CardController.java
package com.example.bankcards.controller;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.utils.SecurityUtils;
import com.example.bankcards.service.card.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cards")
@SecurityRequirement(name = "bearerAuth") // требуем JWT по умолчанию
@RestController
@RequestMapping("/cards")
@Validated
public class CardController {

    private final CardService cards;

    public CardController(CardService cards) {
        this.cards = cards;
    }

    @Operation(summary = "Create card for owner (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/owner/{ownerId}")
    public ResponseEntity<CardResponse> createForOwner(@PathVariable @Positive Long ownerId,
                                                       @RequestBody @Valid CreateCardRequest req) {
        var body = cards.createForOwner(ownerId, req);
        return ResponseEntity.status(201).body(body);
    }

    @Operation(summary = "Admin list cards with filters (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public Page<CardResponse> adminList(@ParameterObject @ModelAttribute CardFilter filter,
                                        @ParameterObject Pageable pageable) {
        return cards.adminList(filter, pageable);
    }

    @Operation(summary = "List my cards")
    @GetMapping("/me")
    public Page<CardResponse> myCards(@RequestParam(value = "status", required = false) CardStatus status,
                                      @ParameterObject Pageable pageable) {

        Long userId = SecurityUtils.currentUserId();
        return cards.findMyCards(userId, status, pageable);
    }


    @Operation(summary = "Change card status (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable @Positive Long id,
                                             @RequestBody @Valid ChangeStatusRequest req) {
        cards.changeStatus(id, req.status());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @Operation(summary = "Update card expiry (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/expiry")
    public ResponseEntity<CardResponse> updateExpiry(@PathVariable @Positive Long id,
                                                     @RequestBody @Valid UpdateExpiryRequest req) {
        var body = cards.updateExpiry(id, req);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Delete card (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        cards.delete(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}