package com.maahfuzdev.moneymanager.recurring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
public class RecurringTransactionController {

    public record ActiveRequest(@NotNull Boolean active) { }

    private final RecurringTransactionService recurringService;

    public RecurringTransactionController(RecurringTransactionService recurringService) {
        this.recurringService = recurringService;
    }

    @GetMapping
    public List<RecurringTransactionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return recurringService.list(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTransactionResponse create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecurringTransactionRequest request) {
        return recurringService.create(jwt.getSubject(), request);
    }

    @PatchMapping("/{id}/active")
    public RecurringTransactionResponse setActive(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ActiveRequest request) {
        return recurringService.setActive(jwt.getSubject(), id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        recurringService.delete(jwt.getSubject(), id);
    }
}
