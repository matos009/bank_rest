package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransferStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.transfer.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock UserRepository users;
    @Mock CardRepository cards;
    @Mock TransferRepository transfers;

    @InjectMocks TransferServiceImpl service;

    private User owner;
    private Card from;
    private Card to;

    @BeforeEach
    void setUp() {
        owner = user(1L, "u@test.com");

        from = card(10L, owner, "1111", (short)12, (short)(YearMonth.now().getYear()+1),
                CardStatus.ACTIVE, new BigDecimal("100.00"));

        to   = card(11L, owner, "2222", (short)12, (short)(YearMonth.now().getYear()+1),
                CardStatus.ACTIVE, new BigDecimal("5.00"));
    }

    // ==== happy path ====
    @Test
    void transfer_success_updatesBalances_andSavesTransfer() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.of(to));

        // захватим, что сохраняется в transfers.save(...)
        ArgumentCaptor<Transfer> cap = ArgumentCaptor.forClass(Transfer.class);
        when(transfers.save(cap.capture())).thenAnswer(inv -> {
            Transfer t = inv.getArgument(0);
            t.setId(100L);
            // сымитируем, что @PrePersist поставит createdAt
            if (t.getCreatedAt() == null) t.setCreatedAt(OffsetDateTime.now());
            return t;
        });

        TransferRequest req = new TransferRequest(10L, 11L, new BigDecimal("30.00"), "coffee");
        TransferResponse resp = service.transfer(1L, req);

        // Проверяем DTO
        assertThat(resp.id()).isEqualTo(100L);
        assertThat(resp.userId()).isEqualTo(1L);
        assertThat(resp.fromCardId()).isEqualTo(10L);
        assertThat(resp.toCardId()).isEqualTo(11L);
        assertThat(resp.amount()).isEqualByComparingTo("30.00");
        assertThat(resp.status()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(resp.description()).isEqualTo("coffee");
        assertThat(resp.createdAt()).isNotNull();

        // Балансы поменялись
        assertThat(from.getBalance()).isEqualByComparingTo("70.00");
        assertThat(to.getBalance()).isEqualByComparingTo("35.00");

        // Что именно сохранили
        Transfer saved = cap.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
        assertThat(saved.getFromCard().getId()).isEqualTo(10L);
        assertThat(saved.getToCard().getId()).isEqualTo(11L);
        assertThat(saved.getAmount()).isEqualByComparingTo("30.00");
        assertThat(saved.getStatus()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(saved.getDescription()).isEqualTo("coffee");

        verify(transfers, times(1)).save(any(Transfer.class));
        verifyNoMoreInteractions(transfers);
    }

    // ==== базовая валидация входа ====
    @Test
    void transfer_sameCard_rejected() {
        TransferRequest req = new TransferRequest(10L, 10L, new BigDecimal("1.00"), null);
        assertThatThrownBy(() -> service.transfer(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("same card");
    }

    @Test
    void transfer_negativeOrZeroAmount_rejected() {
        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("0.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount must be > 0");

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("-5"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount must be > 0");
    }

    @Test
    void transfer_missingIds_rejected() {
        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(null, 11L, new BigDecimal("1"), null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, null, new BigDecimal("1"), null)))
                .isInstanceOf(BusinessException.class);
    }

    // ==== сущности не найдены ====
    @Test
    void transfer_userNotFound() {
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("1.00"), null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void transfer_fromCardNotFound() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("1.00"), null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("From-card not found");
    }

    @Test
    void transfer_toCardNotFound() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("1.00"), null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("To-card not found");
    }

    // ==== чужие карты ====
    @Test
    void transfer_foreignCard_rejected() {
        User other = user(2L, "x@test.com");
        from.setUser(other); // сделаем отправителя чужим

        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("1.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("belong");
    }

    // ==== блокировки/экспирации ====
    @Test
    void transfer_fromBlocked_rejected() {
        from.setStatus(CardStatus.BLOCKED);

        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("10.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void transfer_toExpiredByStatus_rejected() {
        to.setStatus(CardStatus.EXPIRED);

        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("10.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("EXPIRED");
    }

    @Test
    void transfer_fromExpiredByDate_rejected() {
        // Сделаем срок прошедшим
        YearMonth past = YearMonth.now().minusMonths(1);
        from.setExpYear((short) past.getYear());
        from.setExpMonth((short) past.getMonthValue());

        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("10.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("EXPIRED");
    }

    // ==== недостаточно средств ====
    @Test
    void transfer_insufficientFunds_rejected() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(cards.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cards.findByIdForUpdate(11L)).thenReturn(Optional.of(to));

        // Попробуем списать больше, чем есть
        assertThatThrownBy(() -> service.transfer(1L,
                new TransferRequest(10L, 11L, new BigDecimal("1000.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient funds");
    }

    // ===== helpers =====

    private static User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setEnabled(true);
        return u;
    }

    private static Card card(Long id, User owner, String last4, short expMonth, short expYear,
                             CardStatus status, BigDecimal balance) {
        Card c = new Card();
        c.setId(id);
        c.setUser(owner);
        c.setPanLast4(last4);
        c.setExpMonth(expMonth);
        c.setExpYear(expYear);
        c.setStatus(status);
        c.setBalance(balance);
        return c;
    }
}