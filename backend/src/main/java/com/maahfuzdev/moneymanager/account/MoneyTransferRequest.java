package com.maahfuzdev.moneymanager.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MoneyTransferRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull LocalDate transferDate,
        @Size(max = 500) String note) { }
