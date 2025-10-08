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
import com.example.bankcards.security.utils.SecurityUtils;
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

        if (req.fromCardId() == null || req.toCardId() == null) {
            throw new BusinessException("Both fromCardId and toCardId are required");
        }
        if (req.fromCardId().equals(req.toCardId())) {
            throw new BusinessException("Cannot transfer to the same card");
        }
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be > 0");
        }


        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));


        Card from = cards.findByIdForUpdate(req.fromCardId())
                .orElseThrow(() -> new NotFoundException("From-card not found: " + req.fromCardId()));
        Card to   = cards.findByIdForUpdate(req.toCardId())
                .orElseThrow(() -> new NotFoundException("To-card not found: " + req.toCardId()));


        if (!from.getUser().getId().equals(user.getId()) || !to.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Both cards must belong to the user");
        }


        rejectIfBlockedOrExpired(from, "from-card");
        rejectIfBlockedOrExpired(to,   "to-card");


        if (from.getBalance().compareTo(req.amount()) < 0) {
            throw new BusinessException("Insufficient funds");
        }


        from.setBalance(from.getBalance().subtract(req.amount()));
        to.setBalance(to.getBalance().add(req.amount()));


        Transfer t = new Transfer();
        t.setUser(user);
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(req.amount());
        t.setStatus(TransferStatus.SUCCESS);
        t.setDescription(req.description());

        Transfer saved = transfers.save(t);


        return TransferMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TransferResponse transferForCurrentUser(TransferRequest req) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException("Unauthenticated");
        }
        return transfer(userId, req);
    }



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