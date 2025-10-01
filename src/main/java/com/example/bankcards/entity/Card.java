package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity
@Table(name = "cards") @Getter @Setter
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // FK user_id
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pan_enc", nullable = false)
    private byte[] panEnc;

    @Column(name = "pan_fp", nullable = false)
    private byte[] panFp;

    @Column(name = "pan_last4", nullable = false, length = 4)
    private String panLast4;

    @Column(name = "exp_month", nullable = false)
    private short expMonth;

    @Column(name = "exp_year", nullable = false)
    private short expYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void preUpdate() { updatedAt = OffsetDateTime.now(); }

}