package com.maahfuzdev.moneymanager.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MoneyAccountAdjustmentRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal actualBalance,
        @NotNull LocalDate adjustmentDate,
        @Size(max = 300) String note) { }
