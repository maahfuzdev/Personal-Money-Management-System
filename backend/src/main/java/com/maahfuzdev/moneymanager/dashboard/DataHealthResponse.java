package com.maahfuzdev.moneymanager.dashboard;

import com.maahfuzdev.moneymanager.account.AccountType;
import com.maahfuzdev.moneymanager.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DataHealthResponse(Instant checkedAt, BigDecimal averageExpense, BigDecimal largeExpenseThreshold,
        List<DuplicateTransaction> possibleDuplicates, List<LargeExpense> largeExpenses,
        List<NegativeAccount> negativeAccounts) {
    public record DuplicateTransaction(String accountName, String category, String note, LocalDate date,
                                       TransactionType type, BigDecimal amount, long copies) { }
    public record LargeExpense(Long transactionId, String accountName, String category, String note,
                               LocalDate date, BigDecimal amount) { }
    public record NegativeAccount(Long accountId, String name, AccountType type, BigDecimal balance) { }
}
