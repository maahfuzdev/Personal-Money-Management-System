package com.maahfuzdev.moneymanager.dashboard;

import com.maahfuzdev.moneymanager.account.AccountType;
import com.maahfuzdev.moneymanager.account.MoneyAccountAdjustmentRepository;
import com.maahfuzdev.moneymanager.account.MoneyAccountRepository;
import com.maahfuzdev.moneymanager.account.MoneyTransferRepository;
import com.maahfuzdev.moneymanager.transaction.MoneyTransaction;
import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DataHealthService {
    private static final BigDecimal FIVE = BigDecimal.valueOf(5);
    private final AppUserRepository users;
    private final TransactionRepository transactions;
    private final MoneyAccountRepository accounts;
    private final MoneyTransferRepository transfers;
    private final MoneyAccountAdjustmentRepository adjustments;

    public DataHealthService(AppUserRepository users, TransactionRepository transactions, MoneyAccountRepository accounts,
                             MoneyTransferRepository transfers, MoneyAccountAdjustmentRepository adjustments) {
        this.users = users;
        this.transactions = transactions;
        this.accounts = accounts;
        this.transfers = transfers;
        this.adjustments = adjustments;
    }

    public DataHealthResponse check(String email) {
        Long userId = users.findByEmail(email).orElseThrow(AnalyticsAccountNotFoundException::new).getId();
        List<DataHealthResponse.DuplicateTransaction> duplicates = transactions.findDuplicateGroups(userId).stream()
                .map(row -> new DataHealthResponse.DuplicateTransaction((String) row[0], (String) row[1],
                        (String) row[2], ((java.sql.Date) row[3]).toLocalDate(), TransactionType.valueOf((String) row[4]),
                        (BigDecimal) row[5], ((Number) row[6]).longValue())).toList();
        BigDecimal averageExpense = transactions.averageAmountByUserAndType(userId, TransactionType.EXPENSE);
        if (averageExpense == null) averageExpense = BigDecimal.ZERO;
        BigDecimal threshold = averageExpense.multiply(FIVE);
        List<DataHealthResponse.LargeExpense> largeExpenses = averageExpense.signum() == 0 ? List.of()
                : transactions.findLargeTransactions(userId, TransactionType.EXPENSE, threshold, PageRequest.of(0, 10))
                        .stream().map(DataHealthService::largeExpense).toList();
        List<DataHealthResponse.NegativeAccount> negativeAccounts = accounts.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(account -> account.getType() != AccountType.CREDIT_CARD)
                .map(account -> {
                    BigDecimal balance = account.getOpeningBalance()
                            .add(transactions.netAmountByAccount(account.getId(), userId, TransactionType.INCOME))
                            .add(transfers.netTransfers(userId, account.getId()))
                            .add(adjustments.netAdjustments(userId, account.getId()));
                    return balance.signum() < 0 ? new DataHealthResponse.NegativeAccount(
                            account.getId(), account.getName(), account.getType(), balance) : null;
                }).filter(account -> account != null).toList();
        return new DataHealthResponse(Instant.now(), averageExpense, threshold, duplicates, largeExpenses, negativeAccounts);
    }

    private static DataHealthResponse.LargeExpense largeExpense(MoneyTransaction transaction) {
        return new DataHealthResponse.LargeExpense(transaction.getId(), transaction.getAccount().getName(),
                transaction.getCategory(), transaction.getNote(), transaction.getTransactionDate(), transaction.getAmount());
    }
}
