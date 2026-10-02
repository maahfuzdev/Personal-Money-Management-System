package com.maahfuzdev.moneymanager.backup;

import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.recurring.RecurringTransactionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountBackupResponse(int formatVersion, Instant exportedAt,
        List<TransactionBackup> transactions, List<BudgetBackup> budgets,
        List<GoalBackup> goals, List<ContributionBackup> contributions,
        List<RecurringTransactionResponse> recurringTransactions) {
    public record TransactionBackup(Long id, TransactionType type, BigDecimal amount, String category,
                                    String note, LocalDate transactionDate, Instant createdAt) { }
    public record BudgetBackup(Long id, String category, BigDecimal monthlyLimit, LocalDate monthStart) { }
    public record GoalBackup(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount,
                             LocalDate targetDate, String note, Instant createdAt, Instant updatedAt) { }
    public record ContributionBackup(Long id, Long goalId, BigDecimal amount, String note, Instant createdAt) { }
}
