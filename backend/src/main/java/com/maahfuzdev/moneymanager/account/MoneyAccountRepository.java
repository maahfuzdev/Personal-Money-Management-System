package com.maahfuzdev.moneymanager.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MoneyAccountRepository extends JpaRepository<MoneyAccount, Long> {
    List<MoneyAccount> findAllByUserIdOrderByCreatedAtAsc(Long userId);
    Optional<MoneyAccount> findByIdAndUserId(Long id, Long userId);
    Optional<MoneyAccount> findByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    long countByUserId(Long userId);

    @Query("select coalesce(sum(a.openingBalance), 0) from MoneyAccount a where a.user.id = :userId")
    java.math.BigDecimal sumOpeningBalancesByUserId(@Param("userId") Long userId);
}
