package com.maahfuzdev.moneymanager.backup;

import com.maahfuzdev.moneymanager.dashboard.AnalyticsAccountNotFoundException;
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

@Service
@Transactional(readOnly = true)
public class AccountBackupService {
    private final AppUserRepository users;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final SavingsGoalRepository goals;
    private final GoalContributionRepository contributions;
    private final RecurringTransactionRepository recurring;

    public AccountBackupService(AppUserRepository users, TransactionRepository transactions,
            BudgetRepository budgets, SavingsGoalRepository goals, GoalContributionRepository contributions,
            RecurringTransactionRepository recurring) {
        this.users = users; this.transactions = transactions; this.budgets = budgets;
        this.goals = goals; this.contributions = contributions; this.recurring = recurring;
    }

    public AccountBackupResponse export(String email) {
        Long userId = users.findByEmail(email).orElseThrow(AnalyticsAccountNotFoundException::new).getId();
        List<AccountBackupResponse.TransactionBackup> transactionRows = transactions
                .findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(userId).stream().map(t ->
                    new AccountBackupResponse.TransactionBackup(t.getId(), t.getType(), t.getAmount(), t.getCategory(),
                            t.getNote(), t.getTransactionDate(), t.getCreatedAt())).toList();
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
        return new AccountBackupResponse(1, Instant.now(), transactionRows, budgetRows, goalRows, contributionRows, recurringRows);
    }
}
