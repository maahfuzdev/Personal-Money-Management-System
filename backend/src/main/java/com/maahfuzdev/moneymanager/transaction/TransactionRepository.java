package com.maahfuzdev.moneymanager.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface TransactionRepository extends JpaRepository<MoneyTransaction, Long> {

    List<MoneyTransaction> findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    Optional<MoneyTransaction> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(t.amount), 0) from MoneyTransaction t where t.user.id = :userId and t.type = :type")
    BigDecimal sumAmountByUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("select lower(t.category), sum(t.amount) from MoneyTransaction t " +
            "where t.user.id = :userId and t.type = :type and t.transactionDate >= :start and t.transactionDate < :end " +
            "group by lower(t.category)")
    List<Object[]> sumExpensesByCategory(@Param("userId") Long userId, @Param("type") TransactionType type,
                                        @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select min(t.category), sum(t.amount) from MoneyTransaction t " +
            "where t.user.id = :userId and t.type = :type and t.transactionDate >= :start and t.transactionDate < :end " +
            "group by lower(t.category)")
    List<Object[]> summarizeExpensesByCategory(@Param("userId") Long userId, @Param("type") TransactionType type,
                                              @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = "SELECT YEAR(transaction_date), MONTH(transaction_date), type, SUM(amount) " +
            "FROM money_transactions WHERE user_id = :userId AND transaction_date >= :start " +
            "AND transaction_date < :end GROUP BY YEAR(transaction_date), MONTH(transaction_date), type",
            nativeQuery = true)
    List<Object[]> summarizeMonthly(@Param("userId") Long userId, @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);
}
