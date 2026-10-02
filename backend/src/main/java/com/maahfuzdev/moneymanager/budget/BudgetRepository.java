package com.maahfuzdev.moneymanager.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByUserIdAndMonthStartOrderByCategoryAsc(Long userId, LocalDate monthStart);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndCategoryIgnoreCaseAndMonthStart(Long userId, String category, LocalDate monthStart);
    boolean existsByUserIdAndCategoryIgnoreCaseAndMonthStartAndIdNot(
            Long userId, String category, LocalDate monthStart, Long id);
}
