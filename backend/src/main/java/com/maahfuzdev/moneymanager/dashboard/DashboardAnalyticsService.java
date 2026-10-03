package com.maahfuzdev.moneymanager.dashboard;

import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardAnalyticsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;
    private final Clock clock;

    public DashboardAnalyticsService(TransactionRepository transactionRepository,
                                     AppUserRepository userRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public DashboardAnalyticsResponse analytics(String email, String month) {
        Long userId = userRepository.findByEmail(email).orElseThrow(AnalyticsAccountNotFoundException::new).getId();
        YearMonth selectedMonth = month == null ? YearMonth.now(clock) : YearMonth.parse(month);
        LocalDate start = selectedMonth.minusMonths(5).atDay(1);
        LocalDate selectedStart = selectedMonth.atDay(1);
        LocalDate end = selectedMonth.plusMonths(1).atDay(1);

        Map<YearMonth, MonthValues> valuesByMonth = new HashMap<>();
        for (Object[] row : transactionRepository.summarizeMonthly(userId, start, end)) {
            YearMonth rowMonth = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            MonthValues values = valuesByMonth.computeIfAbsent(rowMonth, ignored -> new MonthValues());
            BigDecimal amount = (BigDecimal) row[3];
            if (TransactionType.INCOME.name().equals(row[2])) values.income = amount;
            else values.expense = amount;
        }

        List<MonthTrend> trend = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth period = selectedMonth.minusMonths(offset);
            MonthValues value = valuesByMonth.getOrDefault(period, new MonthValues());
            trend.add(new MonthTrend(period.toString(), value.income, value.expense));
        }

        MonthValues selected = valuesByMonth.getOrDefault(selectedMonth, new MonthValues());
        BigDecimal previousExpense = valuesByMonth.getOrDefault(selectedMonth.minusMonths(1), new MonthValues()).expense;
        BigDecimal expenseChangePercent = previousExpense.signum() == 0 ? null
                : selected.expense.subtract(previousExpense).multiply(BigDecimal.valueOf(100))
                        .divide(previousExpense, 1, RoundingMode.HALF_UP);
        List<Object[]> groupedExpenses = transactionRepository.summarizeExpensesByCategory(
                userId, TransactionType.EXPENSE, selectedStart, end);
        BigDecimal totalExpense = groupedExpenses.stream().map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategorySpending> categories = groupedExpenses.stream()
                .map(row -> new CategorySpending((String) row[0], (BigDecimal) row[1],
                        totalExpense.signum() == 0 ? ZERO : ((BigDecimal) row[1]).multiply(BigDecimal.valueOf(100))
                                .divide(totalExpense, 1, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(CategorySpending::amount).reversed())
                .toList();

        Map<String, BigDecimal> priorThreeMonthTotals = new HashMap<>();
        for (Object[] row : transactionRepository.summarizeExpensesByCategory(userId, TransactionType.EXPENSE,
                selectedMonth.minusMonths(3).atDay(1), selectedStart)) {
            priorThreeMonthTotals.put(((String) row[0]).toLowerCase(Locale.ROOT), (BigDecimal) row[1]);
        }
        List<SpendingAlert> alerts = categories.stream().map(category -> {
                    BigDecimal previousAverage = priorThreeMonthTotals
                            .getOrDefault(category.category().toLowerCase(Locale.ROOT), BigDecimal.ZERO)
                            .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                    if (previousAverage.signum() == 0 || category.amount().compareTo(previousAverage) <= 0) return null;
                    BigDecimal increasePercent = category.amount().subtract(previousAverage)
                            .multiply(BigDecimal.valueOf(100)).divide(previousAverage, 1, RoundingMode.HALF_UP);
                    return increasePercent.compareTo(BigDecimal.valueOf(30)) >= 0
                            ? new SpendingAlert(category.category(), category.amount(), previousAverage, increasePercent)
                            : null;
                })
                .filter(alert -> alert != null)
                .sorted(Comparator.comparing(SpendingAlert::increasePercent).reversed())
                .toList();

        return new DashboardAnalyticsResponse(selectedMonth.toString(), selected.income, selected.expense,
                selected.income.subtract(selected.expense), previousExpense, expenseChangePercent,
                List.copyOf(trend), categories, alerts);
    }

    private static class MonthValues {
        private BigDecimal income = ZERO;
        private BigDecimal expense = ZERO;
    }
}
