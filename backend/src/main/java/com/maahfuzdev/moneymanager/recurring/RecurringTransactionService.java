package com.maahfuzdev.moneymanager.recurring;

import com.maahfuzdev.moneymanager.transaction.MoneyTransaction;
import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Dhaka");
    private static final int CATCH_UP_LIMIT = 120;

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;
    private final Clock clock;

    public RecurringTransactionService(RecurringTransactionRepository recurringRepository,
            TransactionRepository transactionRepository, AppUserRepository userRepository, Clock clock) {
        this.recurringRepository = recurringRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public List<RecurringTransactionResponse> list(String email) {
        AppUser user = user(email);
        return recurringRepository.findAllByUserIdOrderByActiveDescNextRunDateAsc(user.getId())
                .stream().map(RecurringTransactionResponse::from).toList();
    }

    @Transactional
    public RecurringTransactionResponse create(String email, RecurringTransactionRequest request) {
        validateDates(request.startDate(), request.endDate());
        RecurringTransaction saved = recurringRepository.save(new RecurringTransaction(user(email), request));
        return RecurringTransactionResponse.from(saved);
    }

    @Transactional
    public RecurringTransactionResponse setActive(String email, Long id, boolean active) {
        RecurringTransaction recurring = ownedRecurring(email, id);
        if (active && recurring.isFinished()) {
            throw new InvalidRecurringDateException("This recurring schedule has reached its end date");
        }
        recurring.setActive(active);
        return RecurringTransactionResponse.from(recurring);
    }

    @Transactional
    public void delete(String email, Long id) {
        recurringRepository.delete(ownedRecurring(email, id));
    }

    @Transactional
    public int createDueTransactions() {
        LocalDate today = LocalDate.now(clock.withZone(APP_ZONE));
        List<RecurringTransaction> due = recurringRepository.lockDueTransactions(today);
        int created = 0;
        for (RecurringTransaction recurring : due) {
            int caughtUp = 0;
            while (recurring.isActive() && !recurring.getNextRunDate().isAfter(today)
                    && caughtUp < CATCH_UP_LIMIT) {
                LocalDate occurrenceDate = recurring.getNextRunDate();
                if (recurring.getEndDate() != null && occurrenceDate.isAfter(recurring.getEndDate())) {
                    recurring.setActive(false);
                    break;
                }
                transactionRepository.save(new MoneyTransaction(recurring.getUser(), recurring.getType(),
                        recurring.getAmount(), recurring.getCategory(), recurring.getNote(), occurrenceDate));
                recurring.recordOccurrence();
                created++;
                caughtUp++;
            }
        }
        return created;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(clock.withZone(APP_ZONE));
        if (startDate.isBefore(today)) {
            throw new InvalidRecurringDateException("Start date must be today or later");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidRecurringDateException("End date must be on or after the start date");
        }
    }

    private RecurringTransaction ownedRecurring(String email, Long id) {
        return recurringRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(RecurringTransactionNotFoundException::new);
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email).orElseThrow(RecurringTransactionNotFoundException::new);
    }
}
