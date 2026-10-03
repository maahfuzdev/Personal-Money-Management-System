package com.maahfuzdev.moneymanager.backup;

import com.maahfuzdev.moneymanager.dashboard.AnalyticsAccountNotFoundException;
import com.maahfuzdev.moneymanager.account.MoneyAccountRepository;
import com.maahfuzdev.moneymanager.account.MoneyTransferRepository;
import com.maahfuzdev.moneymanager.account.MoneyAccountAdjustmentRepository;
import com.maahfuzdev.moneymanager.account.MoneyAccount;
import com.maahfuzdev.moneymanager.account.MoneyTransfer;
import com.maahfuzdev.moneymanager.account.MoneyAccountAdjustment;
import com.maahfuzdev.moneymanager.budget.Budget;
import com.maahfuzdev.moneymanager.goal.SavingsGoal;
import com.maahfuzdev.moneymanager.goal.GoalContribution;
import com.maahfuzdev.moneymanager.recurring.RecurringTransaction;
import com.maahfuzdev.moneymanager.budget.BudgetRepository;
import com.maahfuzdev.moneymanager.goal.GoalContributionRepository;
import com.maahfuzdev.moneymanager.goal.SavingsGoalRepository;
import com.maahfuzdev.moneymanager.recurring.RecurringTransactionRepository;
import com.maahfuzdev.moneymanager.recurring.RecurringTransactionResponse;
import com.maahfuzdev.moneymanager.transaction.MoneyTransaction;
import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;
import com.maahfuzdev.moneymanager.account.AccountType;

@Service
@Transactional(readOnly = true)
public class AccountBackupService {
    private final AppUserRepository users;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final SavingsGoalRepository goals;
    private final GoalContributionRepository contributions;
    private final RecurringTransactionRepository recurring;
    private final MoneyAccountRepository accounts;
    private final MoneyTransferRepository transfers;
    private final MoneyAccountAdjustmentRepository adjustments;

    public AccountBackupService(AppUserRepository users, TransactionRepository transactions,
            BudgetRepository budgets, SavingsGoalRepository goals, GoalContributionRepository contributions,
            RecurringTransactionRepository recurring, MoneyAccountRepository accounts,
            MoneyTransferRepository transfers, MoneyAccountAdjustmentRepository adjustments) {
        this.users = users; this.transactions = transactions; this.budgets = budgets;
        this.goals = goals; this.contributions = contributions; this.recurring = recurring;
        this.accounts = accounts; this.transfers = transfers; this.adjustments = adjustments;
    }

    public AccountBackupResponse export(String email) {
        Long userId = users.findByEmail(email).orElseThrow(AnalyticsAccountNotFoundException::new).getId();
        List<AccountBackupResponse.AccountBackup> accountRows = accounts.findAllByUserIdOrderByCreatedAtAsc(userId)
                .stream().map(a -> new AccountBackupResponse.AccountBackup(a.getId(), a.getName(), a.getType(), a.getOpeningBalance())).toList();
        List<AccountBackupResponse.TransferBackup> transferRows = transfers.findAllByUserIdOrderByTransferDateDescCreatedAtDesc(userId)
                .stream().map(t -> new AccountBackupResponse.TransferBackup(t.getId(), t.getFromAccount().getId(),
                        t.getToAccount().getId(), t.getAmount(), t.getTransferDate(), t.getNote())).toList();
        List<AccountBackupResponse.AdjustmentBackup> adjustmentRows = adjustments.findAllByUserIdOrderByAdjustmentDateDescCreatedAtDesc(userId)
                .stream().map(a -> new AccountBackupResponse.AdjustmentBackup(a.getId(), a.getAccount().getId(),
                        a.getPreviousBalance(), a.getActualBalance(), a.getAdjustmentDate(), a.getNote())).toList();
        List<AccountBackupResponse.TransactionBackup> transactionRows = transactions
                .findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(userId).stream().map(t ->
                    new AccountBackupResponse.TransactionBackup(t.getId(), t.getType(), t.getAmount(), t.getCategory(),
                            t.getNote(), t.getTransactionDate(), t.getCreatedAt(), t.getAccount().getName())).toList();
        List<AccountBackupResponse.BudgetBackup> budgetRows = budgets.findAllByUserIdOrderByMonthStartDescCategoryAsc(userId)
                .stream().map(b -> new AccountBackupResponse.BudgetBackup(b.getId(), b.getCategory(), b.getMonthlyLimit(), b.getMonthStart())).toList();
        var goalEntities = goals.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<AccountBackupResponse.GoalBackup> goalRows = goalEntities.stream().map(g ->
                new AccountBackupResponse.GoalBackup(g.getId(), g.getName(), g.getTargetAmount(), g.getCurrentAmount(),
                        g.getTargetDate(), g.getNote(), g.getCreatedAt(), g.getUpdatedAt())).toList();
        List<Long> goalIds = goalEntities.stream().map(g -> g.getId()).toList();
        List<AccountBackupResponse.ContributionBackup> contributionRows = goalIds.isEmpty() ? List.of()
                : contributions.findAllByGoalIdInOrderByCreatedAtDesc(goalIds).stream().map(c ->
                    new AccountBackupResponse.ContributionBackup(c.getId(), c.getGoalId(), c.getAmount(), c.getNote(), c.getCreatedAt())).toList();
        List<RecurringTransactionResponse> recurringRows = recurring.findAllByUserIdOrderByActiveDescNextRunDateAsc(userId)
                .stream().map(RecurringTransactionResponse::from).toList();
        return new AccountBackupResponse(2, Instant.now(), accountRows, transferRows, adjustmentRows,
                transactionRows, budgetRows, goalRows, contributionRows, recurringRows);
    }

    @Transactional
    public BackupRestoreResult restore(String email, AccountBackupResponse backup) {
        if (backup == null || (backup.formatVersion() != 1 && backup.formatVersion() != 2))
            throw new InvalidAccountBackupException("This backup file version is not supported.");
        List<AccountBackupResponse.AccountBackup> backupAccounts = safe(backup.accounts());
        List<AccountBackupResponse.TransferBackup> backupTransfers = safe(backup.transfers());
        List<AccountBackupResponse.AdjustmentBackup> backupAdjustments = safe(backup.adjustments());
        List<AccountBackupResponse.TransactionBackup> backupTransactions = safe(backup.transactions());
        List<AccountBackupResponse.BudgetBackup> backupBudgets = safe(backup.budgets());
        List<AccountBackupResponse.GoalBackup> backupGoals = safe(backup.goals());
        List<AccountBackupResponse.ContributionBackup> backupContributions = safe(backup.contributions());
        List<RecurringTransactionResponse> backupRecurring = safe(backup.recurringTransactions());
        long totalRows = (long) backupAccounts.size() + backupTransfers.size() + backupAdjustments.size()
                + backupTransactions.size() + backupBudgets.size() + backupGoals.size()
                + backupContributions.size() + backupRecurring.size();
        if (totalRows > 20_000) throw new InvalidAccountBackupException("A backup can contain at most 20,000 records.");

        var owner = users.findByEmail(email).orElseThrow(AnalyticsAccountNotFoundException::new);
        long userId = owner.getId();
        RestoreCounts counts = new RestoreCounts();

        Map<Long, MoneyAccount> accountMap = new HashMap<>();
        List<MoneyAccount> ownedAccounts = accounts.findAllByUserIdOrderByCreatedAtAsc(userId);
        for (AccountBackupResponse.AccountBackup row : backupAccounts) {
            if (row == null || row.id() == null || row.name() == null || row.type() == null || row.openingBalance() == null)
                throw new InvalidAccountBackupException("The backup contains an invalid account.");
            MoneyAccount account = ownedAccounts.stream().filter(a -> a.getName().equalsIgnoreCase(row.name())).findFirst().orElse(null);
            if (account == null) {
                account = accounts.save(new MoneyAccount(owner, row.name(), row.type(), row.openingBalance()));
                ownedAccounts.add(account);
                counts.imported++;
            } else counts.skipped++;
            accountMap.put(row.id(), account);
        }
        MoneyAccount defaultAccount = ownedAccounts.stream().filter(a -> a.getName().equalsIgnoreCase("Cash")).findFirst().orElse(null);
        if (defaultAccount == null) {
            defaultAccount = accounts.save(new MoneyAccount(owner, "Cash", AccountType.CASH, BigDecimal.ZERO.setScale(2)));
            ownedAccounts.add(defaultAccount);
            counts.imported++;
        }

        List<MoneyTransfer> existingTransfers = transfers.findAllByUserIdOrderByTransferDateDescCreatedAtDesc(userId);
        for (AccountBackupResponse.TransferBackup row : backupTransfers) {
            MoneyAccount from = row == null ? null : accountMap.get(row.fromAccountId());
            MoneyAccount to = row == null ? null : accountMap.get(row.toAccountId());
            if (row == null || from == null || to == null || row.amount() == null || row.transferDate() == null)
                throw new InvalidAccountBackupException("The backup contains a transfer with an unknown account.");
            boolean duplicate = existingTransfers.stream().anyMatch(t -> t.getFromAccount().getId().equals(from.getId())
                    && t.getToAccount().getId().equals(to.getId()) && t.getAmount().compareTo(row.amount()) == 0
                    && t.getTransferDate().equals(row.transferDate()) && Objects.equals(t.getNote(), row.note()));
            if (duplicate) counts.skipped++;
            else { existingTransfers.add(transfers.save(new MoneyTransfer(owner, from, to, row.amount(), row.transferDate(), row.note()))); counts.imported++; }
        }

        List<MoneyAccountAdjustment> existingAdjustments = adjustments.findAllByUserIdOrderByAdjustmentDateDescCreatedAtDesc(userId);
        for (AccountBackupResponse.AdjustmentBackup row : backupAdjustments) {
            MoneyAccount account = row == null ? null : accountMap.get(row.accountId());
            if (account == null && backupAccounts.isEmpty() && row != null) account = ownedAccounts.stream().filter(a -> a.getId().equals(row.accountId())).findFirst().orElse(null);
            if (row == null || account == null || row.previousBalance() == null || row.actualBalance() == null || row.adjustmentDate() == null)
                throw new InvalidAccountBackupException("The backup contains an adjustment with an unknown account.");
            MoneyAccount selectedAccount = account;
            boolean duplicate = existingAdjustments.stream().anyMatch(a -> a.getAccount().getId().equals(selectedAccount.getId())
                    && a.getPreviousBalance().compareTo(row.previousBalance()) == 0 && a.getActualBalance().compareTo(row.actualBalance()) == 0
                    && a.getAdjustmentDate().equals(row.adjustmentDate()) && Objects.equals(a.getNote(), row.note()));
            if (duplicate) counts.skipped++;
            else { existingAdjustments.add(adjustments.save(new MoneyAccountAdjustment(owner, account, row.previousBalance(), row.actualBalance(), row.adjustmentDate(), row.note()))); counts.imported++; }
        }

        for (AccountBackupResponse.TransactionBackup row : backupTransactions) {
            if (row == null || row.type() == null || row.amount() == null || row.category() == null || row.transactionDate() == null)
                throw new InvalidAccountBackupException("The backup contains an invalid transaction.");
            String accountName = row.accountName() == null || row.accountName().isBlank() ? "Cash" : row.accountName();
            MoneyAccount account = ownedAccounts.stream().filter(a -> a.getName().equalsIgnoreCase(accountName)).findFirst().orElse(null);
            if (account == null) throw new InvalidAccountBackupException("The backup references an account that is missing.");
            boolean duplicate = transactions.existsDuplicateInAccount(userId, account.getId(), row.transactionDate(),
                    row.type(), row.amount(), row.category(), row.note());
            if (duplicate) counts.skipped++;
            else { transactions.save(new MoneyTransaction(owner, account, row.type(), row.amount(), row.category(), row.note(), row.transactionDate())); counts.imported++; }
        }

        for (AccountBackupResponse.BudgetBackup row : backupBudgets) {
            if (row == null || row.category() == null || row.monthlyLimit() == null || row.monthStart() == null)
                throw new InvalidAccountBackupException("The backup contains an invalid budget.");
            if (budgets.existsByUserIdAndCategoryIgnoreCaseAndMonthStart(userId, row.category(), row.monthStart())) counts.skipped++;
            else { budgets.save(new Budget(owner, row.category(), row.monthlyLimit(), row.monthStart())); counts.imported++; }
        }

        List<SavingsGoal> existingGoals = goals.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, SavingsGoal> goalMap = new HashMap<>();
        for (AccountBackupResponse.GoalBackup row : backupGoals) {
            if (row == null || row.id() == null || row.name() == null || row.targetAmount() == null || row.currentAmount() == null)
                throw new InvalidAccountBackupException("The backup contains an invalid savings goal.");
            SavingsGoal goal = existingGoals.stream().filter(g -> g.getName().equalsIgnoreCase(row.name())
                    && g.getTargetAmount().compareTo(row.targetAmount()) == 0 && Objects.equals(g.getTargetDate(), row.targetDate())).findFirst().orElse(null);
            if (goal == null) {
                goal = goals.save(new SavingsGoal(owner, row.name(), row.targetAmount(), row.currentAmount(), row.targetDate(), row.note()));
                existingGoals.add(goal); counts.imported++;
            } else counts.skipped++;
            goalMap.put(row.id(), goal);
        }
        List<Long> targetGoalIds = existingGoals.stream().map(SavingsGoal::getId).toList();
        List<GoalContribution> existingContributions = targetGoalIds.isEmpty() ? new ArrayList<>()
                : contributions.findAllByGoalIdInOrderByCreatedAtDesc(targetGoalIds);
        for (AccountBackupResponse.ContributionBackup row : backupContributions) {
            SavingsGoal goal = row == null ? null : goalMap.get(row.goalId());
            if (row == null || goal == null || row.amount() == null)
                throw new InvalidAccountBackupException("The backup contains a contribution for an unknown goal.");
            boolean duplicate = existingContributions.stream().anyMatch(c -> c.getGoalId().equals(goal.getId())
                    && c.getAmount().compareTo(row.amount()) == 0 && Objects.equals(c.getNote(), row.note()));
            if (duplicate) counts.skipped++;
            else { existingContributions.add(contributions.save(new GoalContribution(goal, row.amount(), row.note()))); counts.imported++; }
        }

        List<RecurringTransaction> existingRecurring = recurring.findAllByUserIdOrderByActiveDescNextRunDateAsc(userId);
        for (RecurringTransactionResponse row : backupRecurring) {
            if (row == null || row.type() == null || row.amount() == null || row.category() == null || row.frequency() == null || row.startDate() == null || row.nextRunDate() == null)
                throw new InvalidAccountBackupException("The backup contains an invalid recurring transaction.");
            boolean duplicate = existingRecurring.stream().anyMatch(r -> r.getType() == row.type()
                    && r.getAmount().compareTo(row.amount()) == 0 && r.getCategory().equalsIgnoreCase(row.category())
                    && Objects.equals(r.getNote(), row.note()) && r.getFrequency() == row.frequency()
                    && r.getStartDate().equals(row.startDate()) && r.getNextRunDate().equals(row.nextRunDate())
                    && Objects.equals(r.getEndDate(), row.endDate()) && r.isActive() == row.active());
            if (duplicate) counts.skipped++;
            else { existingRecurring.add(recurring.save(new RecurringTransaction(owner, row))); counts.imported++; }
        }
        return new BackupRestoreResult(counts.imported, counts.skipped);
    }

    private static <T> List<T> safe(List<T> items) { return items == null ? List.of() : items; }
    private static class RestoreCounts { private int imported; private int skipped; }
}
