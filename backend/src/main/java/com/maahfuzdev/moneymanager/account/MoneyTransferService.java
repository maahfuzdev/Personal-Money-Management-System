package com.maahfuzdev.moneymanager.account;

import com.maahfuzdev.moneymanager.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MoneyTransferService {
    private final MoneyTransferRepository transferRepository;
    private final MoneyAccountService accountService;

    public MoneyTransferService(MoneyTransferRepository transferRepository, MoneyAccountService accountService) {
        this.transferRepository = transferRepository;
        this.accountService = accountService;
    }

    public List<MoneyTransferResponse> list(String email) {
        Long userId = accountService.user(email).getId();
        return transferRepository.findAllByUserIdOrderByTransferDateDescCreatedAtDesc(userId).stream()
                .map(MoneyTransferResponse::from).toList();
    }

    @Transactional
    public MoneyTransferResponse create(String email, MoneyTransferRequest request) {
        AppUser owner = accountService.user(email);
        if (request.fromAccountId().equals(request.toAccountId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose two different accounts.");
        MoneyAccount from = accountService.ownedAccount(owner.getId(), request.fromAccountId());
        MoneyAccount to = accountService.ownedAccount(owner.getId(), request.toAccountId());
        MoneyTransfer saved = transferRepository.save(new MoneyTransfer(owner, from, to, request.amount(),
                request.transferDate(), request.note() == null || request.note().isBlank() ? null : request.note().trim()));
        return MoneyTransferResponse.from(saved);
    }
}
