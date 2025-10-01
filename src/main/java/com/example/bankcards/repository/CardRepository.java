package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByUser(User user, Pageable pageable);
    Page<Card> findByUserAndStatus(User user, CardStatus status, Pageable pageable);

    @Query("select c from Card c join fetch c.user where c.id = :id")
    Optional<Card> findWithUserById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id")
    Optional<Card> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c join fetch c.user where c.id = :id")
    Optional<Card> findByIdForUpdate(@Param("id") Long id);


    @Query("""
        select c from Card c
        join fetch c.user u
        where (:status is null or c.status = :status)
          and (:from is null or c.createdAt >= :from)
          and (:to   is null or c.createdAt <  :to)
    """)
    Page<Card> adminFindAll(
            @Param("status") CardStatus status,
            @Param("from")   OffsetDateTime createdFrom,
            @Param("to")     OffsetDateTime createdTo,
            Pageable pageable
    );

    boolean existsByPanFp(byte[] panFp);
}