package com.maahfuzdev.moneymanager.transaction;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final long MAX_EXPORT_ROWS = 10_000;
    private static final Pattern SPREADSHEET_FORMULA_PREFIX = Pattern.compile("(?s)^\\s*[=+@\\-].*");

    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, AppUserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public TransactionPageResponse list(String email, TransactionType type, String search,
                                        LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionDateRangeException();
        }
        AppUser user = user(email);
        String query = search == null || search.isBlank() ? null : search.trim();
        Page<MoneyTransaction> results = transactionRepository.searchByUser(user.getId(), type, query, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))));
        return new TransactionPageResponse(results.map(TransactionResponse::from).getContent(), results.getNumber(),
                results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    public byte[] exportCsv(String email, TransactionType type, String search,
                            LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionDateRangeException();
        }
        Long userId = user(email).getId();
        String query = search == null || search.isBlank() ? null : search.trim();
        Page<MoneyTransaction> firstPage = transactionRepository.searchByUser(userId, type, query,
                startDate, endDate, PageRequest.of(0, EXPORT_PAGE_SIZE,
                        Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))));
        if (firstPage.getTotalElements() > MAX_EXPORT_ROWS) throw new TransactionExportTooLargeException();

        StringBuilder csv = new StringBuilder("\uFEFFDate,Type,Category,Note,Amount (BDT)\r\n");
        appendCsvRows(csv, firstPage.getContent());
        for (int pageNumber = 1; pageNumber < firstPage.getTotalPages(); pageNumber++) {
            List<MoneyTransaction> nextPage = transactionRepository.searchByUser(userId, type, query,
                    startDate, endDate, PageRequest.of(pageNumber, EXPORT_PAGE_SIZE,
                            Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))))
                    .getContent();
            appendCsvRows(csv, nextPage);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRows(StringBuilder csv, List<MoneyTransaction> transactions) {
        for (MoneyTransaction transaction : transactions) {
            csv.append(transaction.getTransactionDate()).append(',')
                    .append(transaction.getType()).append(',')
                    .append(csvValue(transaction.getCategory())).append(',')
                    .append(csvValue(transaction.getNote())).append(',')
                    .append(transaction.getAmount().toPlainString()).append("\r\n");
        }
    }

    private String csvValue(String value) {
        if (value == null) return "\"\"";
        String safeValue = SPREADSHEET_FORMULA_PREFIX.matcher(value).matches() ? "'" + value : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
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
