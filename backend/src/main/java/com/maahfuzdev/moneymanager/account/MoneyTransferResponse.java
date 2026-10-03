package com.maahfuzdev.moneymanager.account;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MoneyTransferResponse(Long id, Long fromAccountId, String fromAccountName,
                                   Long toAccountId, String toAccountName, BigDecimal amount,
                                   LocalDate transferDate, String note) {
    static MoneyTransferResponse from(MoneyTransfer transfer) {
        return new MoneyTransferResponse(transfer.getId(), transfer.getFromAccount().getId(),
                transfer.getFromAccount().getName(), transfer.getToAccount().getId(),
                transfer.getToAccount().getName(), transfer.getAmount(), transfer.getTransferDate(),
                transfer.getNote());
    }
}
