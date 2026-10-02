package com.maahfuzdev.moneymanager.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<MoneyTransaction, Long> {

    List<MoneyTransaction> findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    Optional<MoneyTransaction> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(t.amount), 0) from MoneyTransaction t where t.user.id = :userId and t.type = :type")
    BigDecimal sumAmountByUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type);
}
