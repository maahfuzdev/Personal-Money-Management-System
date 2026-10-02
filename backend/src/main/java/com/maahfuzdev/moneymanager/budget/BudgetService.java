package com.maahfuzdev.moneymanager.budget;

import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final AppUserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public BudgetService(BudgetRepository budgetRepository, AppUserRepository userRepository,
                         TransactionRepository transactionRepository, Clock clock) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public List<BudgetResponse> list(String email, String month) {
        AppUser user = user(email);
        LocalDate start = parseMonth(month).atDay(1);
        LocalDate end = start.plusMonths(1);
        Map<String, BigDecimal> spentByCategory = transactionRepository
                .sumExpensesByCategory(user.getId(), TransactionType.EXPENSE, start, end).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (BigDecimal) row[1]));
        return budgetRepository.findAllByUserIdAndMonthStartOrderByCategoryAsc(user.getId(), start).stream()
                .map(budget -> BudgetResponse.from(budget,
                        spentByCategory.getOrDefault(budget.getCategory().toLowerCase(Locale.ROOT), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public BudgetResponse create(String email, BudgetRequest request) {
        AppUser owner = user(email);
        LocalDate monthStart = parseMonth(request.month()).atDay(1);
        String category = request.category().trim();
        if (budgetRepository.existsByUserIdAndCategoryIgnoreCaseAndMonthStart(owner.getId(), category, monthStart)) {
            throw new BudgetAlreadyExistsException();
        }
        Budget saved = budgetRepository.save(new Budget(owner, category, request.monthlyLimit(), monthStart));
        return BudgetResponse.from(saved, spent(owner.getId(), category, monthStart));
    }

    @Transactional
    public BudgetResponse update(String email, Long id, BudgetRequest request) {
        AppUser owner = user(email);
        Budget budget = ownedBudget(owner.getId(), id);
        LocalDate monthStart = parseMonth(request.month()).atDay(1);
        String category = request.category().trim();
        if (budgetRepository.existsByUserIdAndCategoryIgnoreCaseAndMonthStartAndIdNot(
                owner.getId(), category, monthStart, id)) {
            throw new BudgetAlreadyExistsException();
        }
        budget.update(category, request.monthlyLimit(), monthStart);
        return BudgetResponse.from(budget, spent(owner.getId(), category, monthStart));
    }

    @Transactional
    public void delete(String email, Long id) {
        budgetRepository.delete(ownedBudget(user(email).getId(), id));
    }

    private BigDecimal spent(Long userId, String category, LocalDate start) {
        return transactionRepository.sumExpensesByCategory(userId, TransactionType.EXPENSE, start,
                        start.plusMonths(1)).stream()
                .filter(row -> category.equalsIgnoreCase((String) row[0]))
                .map(row -> (BigDecimal) row[1]).findFirst().orElse(BigDecimal.ZERO);
    }

    private Budget ownedBudget(Long userId, Long id) {
        return budgetRepository.findByIdAndUserId(id, userId).orElseThrow(BudgetNotFoundException::new);
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email).orElseThrow(BudgetNotFoundException::new);
    }

    private YearMonth parseMonth(String month) {
        return month == null ? YearMonth.now(clock) : YearMonth.parse(month);
    }
}
