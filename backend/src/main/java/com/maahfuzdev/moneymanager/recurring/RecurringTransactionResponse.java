package com.maahfuzdev.moneymanager.recurring;

import com.maahfuzdev.moneymanager.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String category,
        String note,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate nextRunDate,
        LocalDate endDate,
        boolean active) {

    public static RecurringTransactionResponse from(RecurringTransaction recurring) {
        return new RecurringTransactionResponse(recurring.getId(), recurring.getType(), recurring.getAmount(),
                recurring.getCategory(), recurring.getNote(), recurring.getFrequency(), recurring.getStartDate(),
                recurring.getNextRunDate(), recurring.getEndDate(), recurring.isActive());
    }
}
