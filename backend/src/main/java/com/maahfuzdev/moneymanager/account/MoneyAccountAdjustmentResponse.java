package com.maahfuzdev.moneymanager.account;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MoneyAccountAdjustmentResponse(Long id, Long accountId, String accountName,
        BigDecimal previousBalance, BigDecimal actualBalance, BigDecimal adjustmentAmount,
        LocalDate adjustmentDate, String note) {
    static MoneyAccountAdjustmentResponse from(MoneyAccountAdjustment adjustment) {
        return new MoneyAccountAdjustmentResponse(adjustment.getId(), adjustment.getAccount().getId(),
                adjustment.getAccount().getName(), adjustment.getPreviousBalance(), adjustment.getActualBalance(),
                adjustment.getAdjustmentAmount(), adjustment.getAdjustmentDate(), adjustment.getNote());
    }
}
