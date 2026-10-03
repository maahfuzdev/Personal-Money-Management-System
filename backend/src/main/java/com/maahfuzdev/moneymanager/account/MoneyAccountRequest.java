package com.maahfuzdev.moneymanager.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MoneyAccountRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull AccountType type,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal openingBalance) { }
