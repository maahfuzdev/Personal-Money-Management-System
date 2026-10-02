package com.maahfuzdev.moneymanager.transaction;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, AppUserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public List<TransactionResponse> list(String email) {
        AppUser user = user(email);
        return transactionRepository.findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(user.getId())
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional
    public TransactionResponse create(String email, TransactionRequest request) {
        AppUser owner = user(email);
        MoneyTransaction transaction = new MoneyTransaction(owner, request.type(), request.amount(),
                request.category().trim(), cleanNote(request.note()), request.transactionDate());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(String email, Long id, TransactionRequest request) {
        MoneyTransaction transaction = ownedTransaction(email, id);
        transaction.update(request.type(), request.amount(), request.category().trim(), cleanNote(request.note()),
                request.transactionDate());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(String email, Long id) {
        transactionRepository.delete(ownedTransaction(email, id));
    }

    public TransactionSummary summary(String email) {
        Long userId = user(email).getId();
        BigDecimal income = transactionRepository.sumAmountByUserAndType(userId, TransactionType.INCOME);
        BigDecimal expense = transactionRepository.sumAmountByUserAndType(userId, TransactionType.EXPENSE);
        return new TransactionSummary(income, expense, income.subtract(expense));
    }

    private MoneyTransaction ownedTransaction(String email, Long id) {
        return transactionRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(TransactionNotFoundException::new);
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
