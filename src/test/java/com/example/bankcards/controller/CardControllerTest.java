package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateExpiryRequest;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.service.card.CardService;
import com.example.bankcards.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.endsWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean CardService cards;

    // ===== helpers =====
    private CardResponse sampleCard(long id, long ownerId, String last4, int m, int y, CardStatus st) {
        return new CardResponse(
                id,
                ownerId,
                "**** **** **** " + last4,
                (short) m,
                (short) y,
                st,
                new BigDecimal("123.45"),
                OffsetDateTime.parse("2025-01-01T10:00:00Z")
        );
    }

    // ---------- POST /cards/admin/owner/{ownerId} ----------
    @Test
    void createForOwner_ok() throws Exception {
        var req = new CreateCardRequest("4111111111111111", (short)12, (short)2030);
        var resp = sampleCard(10L, 42L, "1111", 12, 2030, CardStatus.ACTIVE);

        given(cards.createForOwner(eq(42L), any(CreateCardRequest.class))).willReturn(resp);

        mvc.perform(post("/cards/admin/owner/{ownerId}", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ownerId").value(42))
                .andExpect(jsonPath("$.maskedPan", endsWith("1111")))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---------- GET /cards/admin?status=...&createdFrom=...&createdTo=...&page/size/sort ----------
    @Test
    void adminList_ok() throws Exception {
        var c = sampleCard(5L, 100L, "0000", 1, 2031, CardStatus.BLOCKED);
        var page = new PageImpl<>(List.of(c), PageRequest.of(0, 2, Sort.by("id").descending()), 1);

        given(cards.adminList(any(), any(Pageable.class))).willReturn(page);

        mvc.perform(get("/cards/admin")
                        .param("status", "BLOCKED")
                        .param("createdFrom", "2025-01-01T00:00:00Z")
                        .param("createdTo", "2025-12-31T23:59:59Z")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].status").value("BLOCKED"))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- GET /cards/me?userId=...&status=...&page/size ----------
    @Test
    @WithMockUser(username = "7", roles = "USER")
    void myCards_ok() throws Exception {
        var c1 = sampleCard(1L, 7L, "1234", 12, 2030, CardStatus.ACTIVE);
        var c2 = sampleCard(2L, 7L, "5678", 1, 2031, CardStatus.ACTIVE);
        var page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 10), 2);

        given(cards.findMyCards(eq(7L), eq(CardStatus.ACTIVE), any(Pageable.class)))
                .willReturn(page);

        mvc.perform(get("/cards/me")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].ownerId").value(7))
                .andExpect(jsonPath("$.content[0].maskedPan", endsWith("1234")))
                .andExpect(jsonPath("$.content[1].maskedPan", endsWith("5678")));
    }

    // ---------- PATCH /cards/{id}/status ----------
    @Test
    void changeStatus_ok() throws Exception {
        doNothing().when(cards).changeStatus(77L, CardStatus.BLOCKED);

        // тело для узкого DTO { "status": "BLOCKED" }
        var body = """
                {"status":"BLOCKED"}
                """;

        mvc.perform(patch("/cards/{id}/status", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(cards).changeStatus(77L, CardStatus.BLOCKED);
    }

    // ---------- PATCH /cards/{id}/expiry ----------
    @Test
    void updateExpiry_ok() throws Exception {
        var req = new UpdateExpiryRequest((short) 2, (short) 2032);
        var resp = sampleCard(77L, 5L, "2222", 2, 2032, CardStatus.ACTIVE);

        given(cards.updateExpiry(eq(77L), any(UpdateExpiryRequest.class))).willReturn(resp);

        mvc.perform(patch("/cards/{id}/expiry", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.expMonth").value(2))
                .andExpect(jsonPath("$.expYear").value(2032));
    }

    // ---------- DELETE /cards/{id} ----------
    @Test
    void delete_ok() throws Exception {
        doNothing().when(cards).delete(55L);

        mvc.perform(delete("/cards/{id}", 55L))
                .andExpect(status().isOk());

        verify(cards).delete(55L);
    }
}