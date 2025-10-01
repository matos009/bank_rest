package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardFilter;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateExpiryRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.crypto.CryptoService;
import com.example.bankcards.service.card.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock UserRepository users;
    @Mock CardRepository cards;
    @Mock CryptoService crypto;

    @InjectMocks
    CardServiceImpl service;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setEmail("u@test.com");
        owner.setEnabled(true);
    }

    // ===== createForOwner =====

    @Test
    void createForOwner_ok() {
        // given
        CreateCardRequest req = new CreateCardRequest("4111111111111111", (short)12, (short)2030);
        when(users.findById(10L)).thenReturn(Optional.of(owner));

        byte[] panEnc = new byte[]{0x01, 0x02};
        byte[] panFp  = new byte[]{0x0A, 0x0B};
        when(crypto.encryptPan("4111111111111111")).thenReturn(panEnc);
        when(crypto.fingerprintPan("4111111111111111")).thenReturn(panFp);
        when(crypto.last4("4111111111111111")).thenReturn("1111");

        when(cards.existsByPanFp(panFp)).thenReturn(false);

        Card saved = card(100L, owner, "1111", (short)12, (short)2030, CardStatus.ACTIVE);
        saved.setPanEnc(panEnc);
        saved.setPanFp(panFp);
        when(cards.save(any(Card.class))).thenReturn(saved);

        // when
        CardResponse dto = service.createForOwner(10L, req);

        // then
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.maskedPan()).endsWith("1111");
        assertThat(dto.status()).isEqualTo(CardStatus.ACTIVE);
        verify(cards).save(argThat(c ->
                c.getUser().getId().equals(10L) &&
                        c.getPanLast4().equals("1111") &&
                        c.getStatus() == CardStatus.ACTIVE));
    }

    @Test
    void createForOwner_duplicateByPanFp_conflict() {
        // given
        CreateCardRequest req = new CreateCardRequest("5555444433331111", (short)1, (short)2031);
        when(users.findById(10L)).thenReturn(Optional.of(owner));

        byte[] panFp = new byte[]{0x0D};
        when(crypto.fingerprintPan("5555444433331111")).thenReturn(panFp);
        when(crypto.encryptPan(anyString())).thenReturn(new byte[]{0x01});
        when(crypto.last4(anyString())).thenReturn("1111");

        when(cards.existsByPanFp(panFp)).thenReturn(true);

        // when/then
        assertThatThrownBy(() -> service.createForOwner(10L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Card with same PAN already exists");
        verify(cards, never()).save(any());
    }

    @Test
    void createForOwner_uniqueIndexThrows_conflict() {
        // given
        CreateCardRequest req = new CreateCardRequest("5555444433331111", (short)1, (short)2031);
        when(users.findById(10L)).thenReturn(Optional.of(owner));

        when(crypto.encryptPan(anyString())).thenReturn(new byte[]{0x01});
        when(crypto.fingerprintPan(anyString())).thenReturn(new byte[]{0x02});
        when(crypto.last4(anyString())).thenReturn("1111");

        when(cards.existsByPanFp(any())).thenReturn(false);
        when(cards.save(any(Card.class))).thenThrow(new DataIntegrityViolationException("unique pan_fp"));

        // when/then
        assertThatThrownBy(() -> service.createForOwner(10L, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createForOwner_userNotFound() {
        // given
        when(users.findById(999L)).thenReturn(Optional.empty());
        var req = new CreateCardRequest("4111111111111111", (short)12, (short)2030);

        // when/then
        assertThatThrownBy(() -> service.createForOwner(999L, req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ===== findMyCards =====

    @Test
    void findMyCards_noStatus() {
        when(users.findById(10L)).thenReturn(Optional.of(owner));

        Card c1 = card(1L, owner, "1111", (short)12, (short)2030, CardStatus.ACTIVE);
        Card c2 = card(2L, owner, "2222", (short)1, (short)2031, CardStatus.BLOCKED);
        Page<Card> pg = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 20), 2);
        when(cards.findByUser(eq(owner), any(Pageable.class))).thenReturn(pg);

        Page<CardResponse> res = service.findMyCards(10L, null, PageRequest.of(0, 20));

        assertThat(res.getTotalElements()).isEqualTo(2);
        assertThat(res.getContent()).extracting(CardResponse::maskedPan)
                .allMatch(m -> m.endsWith("1111") || m.endsWith("2222"));
    }

    @Test
    void findMyCards_withStatus() {
        when(users.findById(10L)).thenReturn(Optional.of(owner));

        Card c1 = card(1L, owner, "1111", (short)12, (short)2030, CardStatus.ACTIVE);
        Page<Card> pg = new PageImpl<>(List.of(c1), PageRequest.of(0, 10), 1);
        when(cards.findByUserAndStatus(eq(owner), eq(CardStatus.ACTIVE), any(Pageable.class))).thenReturn(pg);

        Page<CardResponse> res = service.findMyCards(10L, CardStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).status()).isEqualTo(CardStatus.ACTIVE);
    }

    // ===== adminList =====

    @Test
    void adminList_filtersAndMaps() {
        Card c = card(5L, owner, "9999", (short)5, (short)2032, CardStatus.BLOCKED);
        Page<Card> pg = new PageImpl<>(List.of(c), PageRequest.of(0, 5), 1);
        when(cards.adminFindAll(eq(CardStatus.BLOCKED), any(), any(), any())).thenReturn(pg);

        CardFilter filter = new CardFilter(CardStatus.BLOCKED, null, null);
        Page<CardResponse> res = service.adminList(filter, PageRequest.of(0,5));

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).status()).isEqualTo(CardStatus.BLOCKED);
    }

    // ===== getMine =====

    @Test
    void getMine_ok() {
        when(users.findById(10L)).thenReturn(Optional.of(owner));
        Card c = card(77L, owner, "1234", (short)12, (short)2030, CardStatus.ACTIVE);
        when(cards.findWithUserById(77L)).thenReturn(Optional.of(c));

        CardResponse dto = service.getMine(10L, 77L);

        assertThat(dto.id()).isEqualTo(77L);
        assertThat(dto.maskedPan()).endsWith("1234");
    }

    @Test
    void getMine_foreignCard_error() {
        when(users.findById(10L)).thenReturn(Optional.of(owner));

        User other = new User();
        other.setId(99L);
        Card c = card(77L, other, "1234", (short)12, (short)2030, CardStatus.ACTIVE);
        when(cards.findWithUserById(77L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.getMine(10L, 77L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    // ===== changeStatus =====

    @Test
    void changeStatus_ok() {
        Card c = card(1L, owner, "1111", (short)12, (short)2030, CardStatus.BLOCKED);
        when(cards.findById(1L)).thenReturn(Optional.of(c));

        service.changeStatus(1L, CardStatus.ACTIVE);

        assertThat(c.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void changeStatus_expiredToActive_forbidden() {
        Card c = card(1L, owner, "1111", (short)12, (short)2020, CardStatus.EXPIRED);
        when(cards.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.changeStatus(1L, CardStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot activate EXPIRED");
    }

    // ===== updateExpiry =====

    @Test
    void updateExpiry_ok_andReviveFromExpiredIfFuture() {
        // карта была EXPIRED
        Card c = card(50L, owner, "1111", (short)1, (short)2022, CardStatus.EXPIRED);
        when(cards.findWithUserById(50L)).thenReturn(Optional.of(c));

        // делаем будущую дату (на всякий случай возьмём +2 года)
        short newMonth = (short)12;
        short newYear  = (short)(YearMonth.now().getYear() + 2);

        CardResponse dto = service.updateExpiry(50L, new UpdateExpiryRequest(newMonth, newYear));

        assertThat(c.getExpMonth()).isEqualTo(newMonth);
        assertThat(c.getExpYear()).isEqualTo(newYear);
        assertThat(dto.status()).isEqualTo(CardStatus.ACTIVE); // должен «ожить»
    }

    @Test
    void updateExpiry_pastDate_rejected() {
        short oldYear = (short)(YearMonth.now().getYear() - 1);

        assertThatThrownBy(() -> service.updateExpiry(51L, new UpdateExpiryRequest((short)1, oldYear)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("in the past");

        verifyNoInteractions(cards);
    }

    // ===== delete =====

    @Test
    void delete_ok() {
        when(cards.existsById(9L)).thenReturn(true);
        service.delete(9L);
        verify(cards).deleteById(9L);
    }

    @Test
    void delete_notFound() {
        when(cards.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(NotFoundException.class);
        verify(cards, never()).deleteById(anyLong());
    }

    // ===== helpers =====

    private static Card card(Long id, User owner, String last4,
                             short expMonth, short expYear, CardStatus status) {
        Card c = new Card();
        c.setId(id);
        c.setUser(owner);
        c.setPanLast4(last4);
        c.setExpMonth(expMonth);
        c.setExpYear(expYear);
        c.setStatus(status);
        c.setBalance(BigDecimal.ZERO);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }
}