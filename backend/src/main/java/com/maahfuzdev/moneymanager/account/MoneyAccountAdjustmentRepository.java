package com.maahfuzdev.moneymanager.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MoneyAccountAdjustmentRepository extends JpaRepository<MoneyAccountAdjustment, Long> {
    @Query("select coalesce(sum(a.adjustmentAmount), 0) from MoneyAccountAdjustment a " +
            "where a.account.id = :accountId and a.user.id = :userId")
    BigDecimal netAdjustments(@Param("userId") Long userId, @Param("accountId") Long accountId);

    @Query("select coalesce(sum(a.adjustmentAmount), 0) from MoneyAccountAdjustment a where a.user.id = :userId")
    BigDecimal sumAdjustmentsByUserId(@Param("userId") Long userId);

    List<MoneyAccountAdjustment> findAllByUserIdOrderByAdjustmentDateDescCreatedAtDesc(Long userId);
}
