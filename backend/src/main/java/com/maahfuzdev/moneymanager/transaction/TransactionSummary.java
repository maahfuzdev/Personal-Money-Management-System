package com.maahfuzdev.moneymanager.transaction;

import java.math.BigDecimal;

public record TransactionSummary(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance) {
}
