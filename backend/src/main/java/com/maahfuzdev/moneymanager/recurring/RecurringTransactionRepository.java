package com.maahfuzdev.moneymanager.recurring;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findAllByUserIdOrderByActiveDescNextRunDateAsc(Long userId);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select recurring from RecurringTransaction recurring "
            + "where recurring.active = true and recurring.nextRunDate <= :today "
            + "order by recurring.nextRunDate, recurring.id")
    List<RecurringTransaction> lockDueTransactions(@Param("today") LocalDate today);
}
