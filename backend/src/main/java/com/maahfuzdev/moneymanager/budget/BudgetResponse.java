package com.maahfuzdev.moneymanager.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(Long id, String category, BigDecimal monthlyLimit, BigDecimal spent,
                             BigDecimal remaining, String month) {
    static BudgetResponse from(Budget budget, BigDecimal spent) {
        return new BudgetResponse(budget.getId(), budget.getCategory(), budget.getMonthlyLimit(), spent,
                budget.getMonthlyLimit().subtract(spent), budget.getMonthStart().toString().substring(0, 7));
    }
}
