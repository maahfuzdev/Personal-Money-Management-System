package com.maahfuzdev.moneymanager.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardAnalyticsResponse(String selectedMonth, BigDecimal monthIncome, BigDecimal monthExpense,
                                        BigDecimal monthBalance, BigDecimal previousMonthExpense,
                                        BigDecimal expenseChangePercent, List<MonthTrend> monthlyTrend,
                                        List<CategorySpending> expenseByCategory,
                                        List<SpendingAlert> spendingAlerts) {
}
