package com.maahfuzdev.moneymanager.backup;

import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.recurring.RecurringTransactionResponse;
import com.maahfuzdev.moneymanager.account.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountBackupResponse(int formatVersion, Instant exportedAt,
        List<AccountBackup> accounts, List<TransferBackup> transfers, List<AdjustmentBackup> adjustments,
        List<TransactionBackup> transactions, List<BudgetBackup> budgets,
        List<GoalBackup> goals, List<ContributionBackup> contributions,
        List<RecurringTransactionResponse> recurringTransactions) {
    public record AccountBackup(Long id, String name, AccountType type, BigDecimal openingBalance) { }
    public record TransferBackup(Long id, Long fromAccountId, Long toAccountId, BigDecimal amount,
                                 LocalDate transferDate, String note) { }
    public record AdjustmentBackup(Long id, Long accountId, BigDecimal previousBalance, BigDecimal actualBalance,
                                   LocalDate adjustmentDate, String note) { }
    public record TransactionBackup(Long id, TransactionType type, BigDecimal amount, String category,
                                    String note, LocalDate transactionDate, Instant createdAt, String accountName) { }
    public record BudgetBackup(Long id, String category, BigDecimal monthlyLimit, LocalDate monthStart) { }
    public record GoalBackup(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount,
                             LocalDate targetDate, String note, Instant createdAt, Instant updatedAt) { }
    public record ContributionBackup(Long id, Long goalId, BigDecimal amount, String note, Instant createdAt) { }
}
