package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.crypto.CryptoService;
import com.example.bankcards.util.CardMapper;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


import java.time.YearMonth;
import java.util.Objects;

@Service
@Transactional
public class CardServiceImpl implements CardService {

    private final UserRepository users;
    private final CardRepository cards;
    private final CryptoService crypto;

    public CardServiceImpl(UserRepository users, CardRepository cards, CryptoService crypto) {
        this.users = users;
        this.cards = cards;
        this.crypto = crypto;
    }

    @Override
    public CardResponse createForOwner(Long ownerId, CreateCardRequest req) {
        User owner = users.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found: " + ownerId));

        validateExpiry(req.expMonth(), req.expYear());


        byte[] panEnc = crypto.encryptPan(req.pan());
        byte[] panFp  = crypto.fingerprintPan(req.pan());
        String last4  = crypto.last4(req.pan());


        if (cards.existsByPanFp(panFp)) {
            throw new ConflictException("Card with same PAN already exists");
        }


        Card c = new Card();
        c.setUser(owner);
        c.setPanEnc(panEnc);
        c.setPanFp(panFp);
        c.setPanLast4(last4);
        c.setExpMonth(req.expMonth());
        c.setExpYear(req.expYear());
        c.setStatus(CardStatus.ACTIVE);

        try {
            Card saved = cards.save(c);

            return CardMapper.toDto(saved.getUser() == null
                    ? cards.findWithUserById(saved.getId()).orElse(saved)
                    : saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Card with same PAN already exists", e);
        }
    }

    @Override
    public Page<CardResponse> findMyCards(Long userId, CardStatus status, Pageable pageable) {
        User u = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Page<Card> page = (status == null)
                ? cards.findByUser(u, pageable)
                : cards.findByUserAndStatus(u, status, pageable);


        return page.map(c -> {
            c.setUser(u); // безопасно, в контексте одинаковый user
            return CardMapper.toDto(c);
        });
    }

    @Override
    public Page<CardResponse> adminList(CardFilter filter, Pageable pageable) {
        Page<Card> page = cards.adminFindAll(
                filter == null ? null : filter.status(),
                filter == null ? null : filter.createdFrom(),
                filter == null ? null : filter.createdTo(),
                pageable
        );
        return page.map(CardMapper::toDto);
    }

    @Override
    public CardResponse getMine(Long userId, Long cardId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Card c = cards.findWithUserById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

        if (!Objects.equals(c.getUser().getId(), u.getId())) {
            throw new BusinessException("Card does not belong to user");
        }
        return CardMapper.toDto(c);
    }

    @Override
    public void changeStatus(Long cardId, CardStatus newStatus) {
        Card c = cards.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));


        if (c.getStatus() == CardStatus.EXPIRED && newStatus == CardStatus.ACTIVE) {
            throw new BusinessException("Cannot activate EXPIRED card");
        }
        c.setStatus(newStatus); // dirty checking
    }

    @Override
    public CardResponse updateExpiry(Long cardId, UpdateExpiryRequest req) {
        validateExpiry(req.expMonth(), req.expYear());

        Card c = cards.findWithUserById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

        c.setExpMonth(req.expMonth());
        c.setExpYear(req.expYear());


        if (c.getStatus() == CardStatus.EXPIRED && !isExpired(c.getExpMonth(), c.getExpYear())) {
            c.setStatus(CardStatus.ACTIVE);
        }
        return CardMapper.toDto(c);
    }

    @Override
    public void delete(Long cardId) {
        if (!cards.existsById(cardId)) throw new NotFoundException("Card not found: " + cardId);
        cards.deleteById(cardId);
    }

    // ===== helpers =====

    private void validateExpiry(short month, short year) {
        if (month < 1 || month > 12) {
            throw new BusinessException("expMonth must be 1..12");
        }
        if (year < 2000 || year > 2100) {
            throw new BusinessException("expYear must be 2000..2100");
        }
        if (isExpired(month, year)) {
            throw new BusinessException("Card expiry date is in the past");
        }
    }

    private boolean isExpired(short month, short year) {
        // считаем истекшей, если текущий YearMonth > (year, month)
        YearMonth now = YearMonth.now();
        YearMonth card = YearMonth.of(year, month);
        return now.isAfter(card);
    }
}