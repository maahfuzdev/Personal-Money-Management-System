package com.maahfuzdev.moneymanager.account;

import com.maahfuzdev.moneymanager.transaction.TransactionRepository;
import com.maahfuzdev.moneymanager.transaction.TransactionType;
import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MoneyAccountService {
    private static final int MAX_ACCOUNTS = 20;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final MoneyAccountRepository accountRepository;
    private final MoneyTransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;

    public MoneyAccountService(MoneyAccountRepository accountRepository,
                               MoneyTransferRepository transferRepository,
                               TransactionRepository transactionRepository,
                               AppUserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<MoneyAccountResponse> list(String email) {
        AppUser user = user(email);
        ensureCashAccount(user);
        return accountRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(account -> toResponse(user.getId(), account)).toList();
    }

    @Transactional
    public MoneyAccountResponse create(String email, MoneyAccountRequest request) {
        AppUser owner = user(email);
        if (accountRepository.countByUserId(owner.getId()) >= MAX_ACCOUNTS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can create up to 20 accounts.");
        String name = request.name().trim();
        if (accountRepository.existsByUserIdAndNameIgnoreCase(owner.getId(), name))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this name already exists.");
        MoneyAccount saved = accountRepository.save(new MoneyAccount(owner, name, request.type(), request.openingBalance()));
        return toResponse(owner.getId(), saved);
    }

    MoneyAccount ownedAccount(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    AppUser user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account owner not found."));
    }

    private MoneyAccountResponse toResponse(Long userId, MoneyAccount account) {
        BigDecimal balance = account.getOpeningBalance()
                .add(transactionRepository.netAmountByAccount(account.getId(), userId, TransactionType.INCOME))
                .add(transferRepository.netTransfers(userId, account.getId()));
        return new MoneyAccountResponse(account.getId(), account.getName(), account.getType(),
                account.getOpeningBalance(), balance);
    }

    private void ensureCashAccount(AppUser owner) {
        accountRepository.findByUserIdAndNameIgnoreCase(owner.getId(), "Cash").orElseGet(() ->
                accountRepository.save(new MoneyAccount(owner, "Cash", AccountType.CASH, ZERO)));
    }
}
