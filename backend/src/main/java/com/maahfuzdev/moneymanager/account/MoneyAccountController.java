package com.maahfuzdev.moneymanager.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class MoneyAccountController {
    private final MoneyAccountService accountService;
    private final MoneyTransferService transferService;

    public MoneyAccountController(MoneyAccountService accountService, MoneyTransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @GetMapping
    public List<MoneyAccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return accountService.list(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MoneyAccountResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MoneyAccountRequest request) {
        return accountService.create(jwt.getSubject(), request);
    }

    @GetMapping("/transfers")
    public List<MoneyTransferResponse> transfers(@AuthenticationPrincipal Jwt jwt) {
        return transferService.list(jwt.getSubject());
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public MoneyTransferResponse transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MoneyTransferRequest request) {
        return transferService.create(jwt.getSubject(), request);
    }
}
