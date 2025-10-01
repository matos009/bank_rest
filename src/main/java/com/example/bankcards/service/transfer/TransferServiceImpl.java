package com.example.bankcards.service.transfer;


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
import com.example.bankcards.util.TransferMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@Transactional
public class TransferServiceImpl implements TransferService {

    private final UserRepository users;
    private final CardRepository cards;
    private final TransferRepository transfers;

    public TransferServiceImpl(UserRepository users,
                               CardRepository cards,
                               TransferRepository transfers) {
        this.users = users;
        this.cards = cards;
        this.transfers = transfers;
    }

    @Override
    public TransferResponse transfer(Long userId, TransferRequest req) {
        // 1) базовая валидация входных данных
        if (req.fromCardId() == null || req.toCardId() == null) {
            throw new BusinessException("Both fromCardId and toCardId are required");
        }
        if (req.fromCardId().equals(req.toCardId())) {
            throw new BusinessException("Cannot transfer to the same card");
        }
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be > 0");
        }

        // 2) убедимся, что пользователь существует
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        // 3) поднимем обе карты С БЛОКИРОВКОЙ
        Card from = cards.findByIdForUpdate(req.fromCardId())
                .orElseThrow(() -> new NotFoundException("From-card not found: " + req.fromCardId()));
        Card to   = cards.findByIdForUpdate(req.toCardId())
                .orElseThrow(() -> new NotFoundException("To-card not found: " + req.toCardId()));

        // 4) проверки принадлежности
        if (!from.getUser().getId().equals(user.getId()) || !to.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Both cards must belong to the user");
        }

        // 5) проверки статуса/сроков
        rejectIfBlockedOrExpired(from, "from-card");
        rejectIfBlockedOrExpired(to,   "to-card");

        // 6) проверка достаточности средств
        if (from.getBalance().compareTo(req.amount()) < 0) {
            throw new BusinessException("Insufficient funds");
        }

        // 7) проводка  — внутри одной транзакции + под блокировкой
        from.setBalance(from.getBalance().subtract(req.amount()));
        to.setBalance(to.getBalance().add(req.amount()));

        // 8) запись о переводе
        Transfer t = new Transfer();
        t.setUser(user);
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(req.amount());
        t.setStatus(TransferStatus.SUCCESS);
        t.setDescription(req.description());

        Transfer saved = transfers.save(t);
        // dirty checking сохранит изменения баланса карт при коммите транзакции

        return TransferMapper.toDto(saved);
    }

    // ===== helpers =====

    private void rejectIfBlockedOrExpired(Card c, String label) {
        if (c.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException(label + " is BLOCKED");
        }
        if (c.getStatus() == CardStatus.EXPIRED || isExpired(c)) {
            throw new BusinessException(label + " is EXPIRED");
        }
    }

    private boolean isExpired(Card c) {
        YearMonth now = YearMonth.now();
        YearMonth expiry = YearMonth.of(c.getExpYear(), c.getExpMonth());
        return now.isAfter(expiry);
    }
}