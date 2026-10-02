package com.maahfuzdev.moneymanager.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

public record SavingsGoalResponse(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount,
                                 BigDecimal remainingAmount, BigDecimal completionPercent, boolean completed,
                                 LocalDate targetDate, String note, Instant createdAt, Instant updatedAt) {
    static SavingsGoalResponse from(SavingsGoal goal) {
        BigDecimal percent = goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        return new SavingsGoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount(),
                remaining, percent, goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0,
                goal.getTargetDate(), goal.getNote(), goal.getCreatedAt(), goal.getUpdatedAt());
    }
}
