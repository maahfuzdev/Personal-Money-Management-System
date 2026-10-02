package com.maahfuzdev.moneymanager.dashboard;

import java.math.BigDecimal;

public record CategorySpending(String category, BigDecimal amount, BigDecimal sharePercent) {
}
