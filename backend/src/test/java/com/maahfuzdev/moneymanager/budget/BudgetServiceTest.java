package com.maahfuzdev.moneymanager.budget;

import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgets;
    @Mock private AppUserRepository users;
    @Mock private TransactionRepository transactions;

    private BudgetService service;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgets, users, transactions,
                Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"), ZoneOffset.UTC));
        owner = new AppUser("Amina", "amina@example.com", "password-hash");
    }

    @Test
    void listCalculatesSpentAndRemainingForSelectedMonth() {
        LocalDate monthStart = LocalDate.parse("2026-10-01");
        when(users.findByEmail("amina@example.com")).thenReturn(Optional.of(owner));
        when(transactions.sumExpensesByCategory(isNull(), any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"food", new BigDecimal("45.20")}));
        when(budgets.findAllByUserIdAndMonthStartOrderByCategoryAsc(null, monthStart))
                .thenReturn(List.of(new Budget(owner, "Food", new BigDecimal("100.00"), monthStart)));

        BudgetResponse response = service.list("amina@example.com", "2026-10").get(0);

        assertEquals(new BigDecimal("45.20"), response.spent());
        assertEquals(new BigDecimal("54.80"), response.remaining());
    }

    @Test
    void duplicateCategoryAndMonthIsRejected() {
        when(users.findByEmail("amina@example.com")).thenReturn(Optional.of(owner));
        when(budgets.existsByUserIdAndCategoryIgnoreCaseAndMonthStart(null, "Food", LocalDate.parse("2026-10-01")))
                .thenReturn(true);

        assertThrows(BudgetAlreadyExistsException.class, () -> service.create("amina@example.com",
                new BudgetRequest("Food", new BigDecimal("100.00"), "2026-10")));

        verify(budgets, never()).save(any(Budget.class));
    }
}
