package com.maahfuzdev.moneymanager.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface MoneyTransferRepository extends JpaRepository<MoneyTransfer, Long> {
    @Query("select coalesce(sum(case when t.toAccount.id = :accountId then t.amount else -t.amount end), 0) " +
            "from MoneyTransfer t where t.user.id = :userId " +
            "and (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)")
    BigDecimal netTransfers(@Param("userId") Long userId, @Param("accountId") Long accountId);

    java.util.List<MoneyTransfer> findAllByUserIdOrderByTransferDateDescCreatedAtDesc(Long userId);
}
