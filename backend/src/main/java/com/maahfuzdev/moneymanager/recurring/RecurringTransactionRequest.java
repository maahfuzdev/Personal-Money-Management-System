package com.maahfuzdev.moneymanager.recurring;

import com.maahfuzdev.moneymanager.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(
        @NotNull TransactionType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 60) String category,
        @Size(max = 500) String note,
        @NotNull RecurrenceFrequency frequency,
        @NotNull LocalDate startDate,
        LocalDate endDate) {
}
