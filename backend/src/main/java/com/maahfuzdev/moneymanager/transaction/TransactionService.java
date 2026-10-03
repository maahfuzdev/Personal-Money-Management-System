package com.maahfuzdev.moneymanager.transaction;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import com.maahfuzdev.moneymanager.account.AccountType;
import com.maahfuzdev.moneymanager.account.MoneyAccount;
import com.maahfuzdev.moneymanager.account.MoneyAccountRepository;
import com.maahfuzdev.moneymanager.account.MoneyAccountAdjustmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final long MAX_EXPORT_ROWS = 10_000;
    private static final Pattern SPREADSHEET_FORMULA_PREFIX = Pattern.compile("(?s)^\\s*[=+@\\-].*");

    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;
    private final MoneyAccountRepository accountRepository;
    private final MoneyAccountAdjustmentRepository adjustmentRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, AppUserRepository userRepository,
                              MoneyAccountRepository accountRepository,
                              MoneyAccountAdjustmentRepository adjustmentRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    public TransactionService(TransactionRepository transactionRepository, AppUserRepository userRepository) {
        this(transactionRepository, userRepository, null, null);
    }

    public TransactionPageResponse list(String email, TransactionType type, String search,
                                        LocalDate startDate, LocalDate endDate, int page, int size) {
        return list(email, null, type, search, startDate, endDate, page, size);
    }

    public TransactionPageResponse list(String email, Long accountId, TransactionType type, String search,
                                        LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionDateRangeException();
        }
        AppUser user = user(email);
        String query = search == null || search.isBlank() ? null : search.trim();
        Page<MoneyTransaction> results = searchTransactions(user.getId(), accountId, type, query, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))));
        return new TransactionPageResponse(results.map(TransactionResponse::from).getContent(), results.getNumber(),
                results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    public List<String> categorySuggestions(String email, TransactionType type) {
        return transactionRepository.findCategorySuggestions(user(email).getId(), type);
    }

    public byte[] exportCsv(String email, TransactionType type, String search,
                            LocalDate startDate, LocalDate endDate) {
        return exportCsv(email, null, type, search, startDate, endDate);
    }

    public byte[] exportCsv(String email, Long accountId, TransactionType type, String search,
                            LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionDateRangeException();
        }
        Long userId = user(email).getId();
        String query = search == null || search.isBlank() ? null : search.trim();
        Page<MoneyTransaction> firstPage = searchTransactions(userId, accountId, type, query,
                startDate, endDate, PageRequest.of(0, EXPORT_PAGE_SIZE,
                        Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))));
        if (firstPage.getTotalElements() > MAX_EXPORT_ROWS) throw new TransactionExportTooLargeException();

        StringBuilder csv = new StringBuilder("\uFEFFDate,Type,Category,Note,Amount (BDT),Account\r\n");
        appendCsvRows(csv, firstPage.getContent());
        for (int pageNumber = 1; pageNumber < firstPage.getTotalPages(); pageNumber++) {
            List<MoneyTransaction> nextPage = searchTransactions(userId, accountId, type, query,
                    startDate, endDate, PageRequest.of(pageNumber, EXPORT_PAGE_SIZE,
                            Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))))
                    .getContent();
            appendCsvRows(csv, nextPage);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public TransactionImportPreviewResponse previewCsv(String email, byte[] bytes) {
        AppUser owner = user(email);
        String csv = new String(bytes, StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", "");
        List<List<String>> records = parseCsv(csv);
        List<String> header = records.isEmpty() ? List.of() : records.get(0).stream().map(String::trim).toList();
        boolean legacyHeader = header.equals(List.of("Date", "Type", "Category", "Note", "Amount (BDT)"));
        boolean accountHeader = header.equals(List.of("Date", "Type", "Category", "Note", "Amount (BDT)", "Account"));
        if (!legacyHeader && !accountHeader)
            throw new InvalidTransactionCsvException("CSV header must match the exported transaction format.");
        if (records.size() - 1 > 1000) throw new InvalidTransactionCsvException("CSV can contain at most 1,000 transactions.");
        List<TransactionImportPreview> rows = new ArrayList<>(); Set<String> seen = new HashSet<>();
        int valid = 0, duplicates = 0, invalid = 0;
        for (int i = 1; i < records.size(); i++) {
            List<String> cells = records.get(i);
            if (cells.stream().allMatch(String::isBlank)) continue;
            List<String> errors = new ArrayList<>();
            if (cells.size() != (accountHeader ? 6 : 5)) {
                errors.add(accountHeader ? "Expected six columns." : "Expected five columns.");
                rows.add(new TransactionImportPreview(i + 1, String.join(",", cells), "", "", "", "", false, errors, null)); invalid++; continue;
            }
            String dateText = cells.get(0).trim(), typeText = cells.get(1).trim().toUpperCase();
            String category = cells.get(2).trim(), note = cells.get(3).trim(), amountText = cells.get(4).trim();
            String accountName = accountHeader ? cells.get(5).trim() : "";
            Long accountId = null;
            if (!accountName.isBlank()) {
                accountId = accountRepository.findByUserIdAndNameIgnoreCase(owner.getId(), accountName)
                        .map(MoneyAccount::getId).orElse(null);
                if (accountId == null) errors.add("Account '" + accountName + "' was not found.");
            }
            LocalDate date = null; TransactionType type = null; BigDecimal amount = null;
            try { date = LocalDate.parse(dateText); } catch (RuntimeException ex) { errors.add("Date must use YYYY-MM-DD."); }
            try { type = TransactionType.valueOf(typeText); } catch (RuntimeException ex) { errors.add("Type must be INCOME or EXPENSE."); }
            try { amount = new BigDecimal(amountText); if (amount.scale() > 2 || amount.signum() <= 0 || amount.precision() - amount.scale() > 17) throw new NumberFormatException(); }
            catch (RuntimeException ex) { errors.add("Amount must be positive with at most two decimal places."); }
            if (category.isBlank() || category.length() > 60) errors.add("Category is required and must be at most 60 characters.");
            if (note.length() > 500) errors.add("Note must be at most 500 characters.");
            TransactionRequest transaction = errors.isEmpty() ? new TransactionRequest(accountId, type, amount, category, note.isBlank() ? null : note, date) : null;
            boolean duplicate = false;
            if (transaction != null) {
                String key = date + "|" + type + "|" + amount.stripTrailingZeros() + "|" + category.toLowerCase() + "|" + (transaction.note() == null ? "" : transaction.note()) + "|" + accountId;
                duplicate = !seen.add(key) || transactionRepository.existsDuplicate(owner.getId(), date, type, amount, category, transaction.note());
                if (duplicate) duplicates++; else valid++;
            } else invalid++;
            rows.add(new TransactionImportPreview(i + 1, dateText, typeText, category, note, amountText, duplicate, errors, transaction));
        }
        return new TransactionImportPreviewResponse(valid, duplicates, invalid, rows);
    }

    @Transactional
    public TransactionImportResult importTransactions(String email, List<TransactionRequest> requests) {
        AppUser owner = user(email); int imported = 0, duplicates = 0; Set<String> seen = new HashSet<>();
        for (TransactionRequest request : requests) {
            String category = request.category().trim(), note = cleanNote(request.note());
            String key = request.transactionDate() + "|" + request.type() + "|" + request.amount().stripTrailingZeros() + "|" + category.toLowerCase() + "|" + (note == null ? "" : note) + "|" + request.accountId();
            if (!seen.add(key) || transactionRepository.existsDuplicate(owner.getId(), request.transactionDate(), request.type(), request.amount(), category, note)) { duplicates++; continue; }
            transactionRepository.save(new MoneyTransaction(owner, accountFor(owner, request.accountId()), request.type(), request.amount(), category, note, request.transactionDate())); imported++;
        }
        return new TransactionImportResult(imported, duplicates);
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else if (c == '"') quoted = false; else cell.append(c);
            } else if (c == '"' && cell.length() == 0) quoted = true;
            else if (c == ',') { row.add(cell.toString()); cell.setLength(0); }
            else if (c == '\n' || c == '\r') { if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++; row.add(cell.toString()); cell.setLength(0); rows.add(row); row = new ArrayList<>(); }
            else cell.append(c);
        }
        if (quoted) throw new InvalidTransactionCsvException("CSV contains an unclosed quoted field.");
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }

    private void appendCsvRows(StringBuilder csv, List<MoneyTransaction> transactions) {
        for (MoneyTransaction transaction : transactions) {
            csv.append(transaction.getTransactionDate()).append(',')
                    .append(transaction.getType()).append(',')
                    .append(csvValue(transaction.getCategory())).append(',')
                    .append(csvValue(transaction.getNote())).append(',')
                    .append(transaction.getAmount().toPlainString()).append(',')
                    .append(csvValue(transaction.getAccount().getName())).append("\r\n");
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
        MoneyTransaction transaction = new MoneyTransaction(owner, accountFor(owner, request.accountId()), request.type(), request.amount(),
                request.category().trim(), cleanNote(request.note()), request.transactionDate());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(String email, Long id, TransactionRequest request) {
        MoneyTransaction transaction = ownedTransaction(email, id);
        transaction.update(accountFor(transaction.getAccount().getUser().getId(), request.accountId()), request.type(), request.amount(), request.category().trim(), cleanNote(request.note()),
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
        BigDecimal openingBalances = accountRepository.sumOpeningBalancesByUserId(userId);
        BigDecimal adjustments = adjustmentRepository.sumAdjustmentsByUserId(userId);
        return new TransactionSummary(income, expense, income.subtract(expense).add(openingBalances).add(adjustments));
    }

    private MoneyTransaction ownedTransaction(String email, Long id) {
        return transactionRepository.findByIdAndUserId(id, user(email).getId())
                .orElseThrow(TransactionNotFoundException::new);
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private MoneyAccount accountFor(AppUser owner, Long accountId) {
        return accountFor(owner.getId(), accountId);
    }

    private MoneyAccount accountFor(Long userId, Long accountId) {
        if (accountId == null) return defaultAccount(userRepository.findById(userId).orElseThrow(TransactionNotFoundException::new));
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Account not found."));
    }

    private MoneyAccount defaultAccount(AppUser owner) {
        if (accountRepository == null) return new MoneyAccount(owner, "Cash", AccountType.CASH, BigDecimal.ZERO.setScale(2));
        return accountRepository.findByUserIdAndNameIgnoreCase(owner.getId(), "Cash")
                .orElseGet(() -> accountRepository.save(new MoneyAccount(owner, "Cash", AccountType.CASH, BigDecimal.ZERO.setScale(2))));
    }

    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private Page<MoneyTransaction> searchTransactions(Long userId, Long accountId, TransactionType type,
            String query, LocalDate startDate, LocalDate endDate, org.springframework.data.domain.Pageable pageable) {
        if (accountId == null) {
            return transactionRepository.searchByUser(userId, type, query, startDate, endDate, pageable);
        }
        if (accountRepository.findByIdAndUserId(accountId, userId).isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Account not found.");
        }
        return transactionRepository.searchByUserAndAccount(userId, accountId, type, query,
                startDate, endDate, pageable);
    }
}
