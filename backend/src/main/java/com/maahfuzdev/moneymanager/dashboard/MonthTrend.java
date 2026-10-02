package com.maahfuzdev.moneymanager.dashboard;

import java.math.BigDecimal;

public record MonthTrend(String month, BigDecimal income, BigDecimal expense) {
    public BigDecimal net() { return income.subtract(expense); }
}
