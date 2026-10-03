package com.maahfuzdev.moneymanager.dashboard;

import java.math.BigDecimal;

public record SpendingAlert(String category, BigDecimal currentAmount, BigDecimal threeMonthAverage,
                            BigDecimal increasePercent) {
}
