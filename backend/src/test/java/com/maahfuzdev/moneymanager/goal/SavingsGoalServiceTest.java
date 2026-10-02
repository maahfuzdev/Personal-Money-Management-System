package com.maahfuzdev.moneymanager.goal;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock private SavingsGoalRepository goals;
    @Mock private AppUserRepository users;

    private SavingsGoalService service;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        service = new SavingsGoalService(goals, users);
        owner = new AppUser("Amina", "amina@example.com", "password-hash");
    }

    @Test
    void currentAmountAboveTargetIsRejectedBeforePersistence() {
        assertThrows(SavingsGoalAmountException.class, () -> service.create("amina@example.com",
                new SavingsGoalRequest("Emergency fund", new BigDecimal("100.00"),
                        new BigDecimal("101.00"), null, null)));

        verify(users, never()).findByEmail(any());
        verify(goals, never()).save(any(SavingsGoal.class));
    }

    @Test
    void createTrimsGoalNameAndNormalizesBlankNote() {
        when(users.findByEmail("amina@example.com")).thenReturn(Optional.of(owner));
        when(goals.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SavingsGoalResponse response = service.create("amina@example.com",
                new SavingsGoalRequest("  Emergency fund  ", new BigDecimal("100.00"),
                        new BigDecimal("25.00"), null, "   "));

        assertEquals("Emergency fund", response.name());
        assertEquals(null, response.note());
        assertEquals(new BigDecimal("25.00"), response.currentAmount());
        assertEquals(new BigDecimal("75.00"), response.remainingAmount());
        assertEquals(new BigDecimal("25.00"), response.completionPercent());
    }
}
