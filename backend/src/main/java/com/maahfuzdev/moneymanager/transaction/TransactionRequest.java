package com.maahfuzdev.moneymanager.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        Long accountId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 60) String category,
        @Size(max = 500) String note,
        @NotNull LocalDate transactionDate) {
    public TransactionRequest(TransactionType type, BigDecimal amount, String category, String note,
                              LocalDate transactionDate) {
        this(null, type, amount, category, note, transactionDate);
    }
}
