package com.maahfuzdev.moneymanager.account;

import java.math.BigDecimal;

public record MoneyAccountResponse(Long id, String name, AccountType type,
                                   BigDecimal openingBalance, BigDecimal balance) { }
