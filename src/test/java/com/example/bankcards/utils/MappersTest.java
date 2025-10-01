package com.example.bankcards.utils;


import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransferStatus;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.TransferMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MappersTest {

    // ---------- CardMapper ----------

    @Test
    void cardMapper_mask_returnsMaskedPan() {
        String masked = CardMapper.mask("1234");
        assertThat(masked).isEqualTo("**** **** **** 1234");
    }

    @Test
    void cardMapper_toDto_mapsAllFields() {
        User u = new User();
        u.setId(7L);

        Card c = new Card();
        c.setId(55L);
        c.setUser(u);
        c.setPanLast4("9876");
        c.setExpMonth((short) 12);
        c.setExpYear((short) 2030);
        c.setStatus(CardStatus.ACTIVE);
        c.setBalance(new BigDecimal("123.45"));
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        c.setCreatedAt(created);

        CardResponse dto = CardMapper.toDto(c);

        assertThat(dto.id()).isEqualTo(55L);
        assertThat(dto.ownerId()).isEqualTo(7L);
        assertThat(dto.maskedPan()).isEqualTo("**** **** **** 9876");
        assertThat(dto.expMonth()).isEqualTo((short)12);
        assertThat(dto.expYear()).isEqualTo((short)2030);
        assertThat(dto.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(dto.balance()).isEqualByComparingTo("123.45");
        assertThat(dto.createdAt()).isEqualTo(created);
    }

    // ---------- TransferMapper ----------

    @Test
    void transferMapper_toDto_mapsAllFields() {
        User u = new User(); u.setId(1L);

        Card from = new Card(); from.setId(10L);
        Card to   = new Card(); to.setId(11L);

        Transfer t = new Transfer();
        t.setId(100L);
        t.setUser(u);
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(new BigDecimal("50.00"));
        t.setStatus(TransferStatus.SUCCESS);
        t.setDescription("test");
        OffsetDateTime created = OffsetDateTime.now();
        t.setCreatedAt(created);

        TransferResponse dto = TransferMapper.toDto(t);

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.fromCardId()).isEqualTo(10L);
        assertThat(dto.toCardId()).isEqualTo(11L);
        assertThat(dto.amount()).isEqualByComparingTo("50.00");
        assertThat(dto.status()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(dto.description()).isEqualTo("test");
        assertThat(dto.createdAt()).isEqualTo(created);
    }
}