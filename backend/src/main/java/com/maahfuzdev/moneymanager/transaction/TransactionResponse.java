package com.maahfuzdev.moneymanager.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(Long id, Long accountId, String accountName, TransactionType type, BigDecimal amount, String category,
                                 String note, LocalDate transactionDate, Instant createdAt) {
    static TransactionResponse from(MoneyTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(), transaction.getAccount().getName(),
                transaction.getType(), transaction.getAmount(),
                transaction.getCategory(), transaction.getNote(), transaction.getTransactionDate(),
                transaction.getCreatedAt());
    }
}
