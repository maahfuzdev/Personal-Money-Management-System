package com.maahfuzdev.moneymanager.transaction;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactions;
    @Mock private AppUserRepository users;

    private TransactionService service;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactions, users);
        owner = new AppUser("Amina", "amina@example.com", "password-hash");
    }

    @Test
    void reversedDateRangeIsRejectedBeforeDatabaseAccess() {
        assertThrows(InvalidTransactionDateRangeException.class, () -> service.list(
                "amina@example.com", null, null, LocalDate.parse("2026-10-03"),
                LocalDate.parse("2026-10-02"), 0, 10));

        verify(users, never()).findByEmail(anyString());
        verify(transactions, never()).searchByUser(anyLong(), any(), any(), any(), any(), any(PageRequest.class));
    }

    @Test
    void createTrimsCategoryAndConvertsBlankNoteToNull() {
        when(users.findByEmail("amina@example.com")).thenReturn(Optional.of(owner));
        when(transactions.save(any(MoneyTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse created = service.create("amina@example.com", new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("45.20"), "  Food  ", "   ", LocalDate.parse("2026-10-02")));

        assertEquals("Food", created.category());
        assertEquals(null, created.note());
        assertEquals(new BigDecimal("45.20"), created.amount());
    }

    @Test
    void csvExportPrefixesFormulaTextAndIncludesUtf8Bom() {
        when(users.findByEmail("amina@example.com")).thenReturn(Optional.of(owner));
        MoneyTransaction transaction = new MoneyTransaction(owner, TransactionType.EXPENSE,
                new BigDecimal("12.50"), "=SUM(1,2)", "=HYPERLINK(\"https://example.com\")", LocalDate.parse("2026-10-02"));
        when(transactions.searchByUser(isNull(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        String csv = new String(service.exportCsv("amina@example.com", null, null, null, null), StandardCharsets.UTF_8);

        assertEquals('\uFEFF', csv.charAt(0));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("\"'=SUM(1,2)\""));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("\"'=HYPERLINK(\"\"https://example.com\"\")\""));
    }
}
