package com.maahfuzdev.moneymanager.api;

import com.maahfuzdev.moneymanager.auth.EmailAlreadyRegisteredException;
import com.maahfuzdev.moneymanager.auth.InvalidCredentialsException;
import com.maahfuzdev.moneymanager.budget.BudgetAlreadyExistsException;
import com.maahfuzdev.moneymanager.budget.BudgetNotFoundException;
import com.maahfuzdev.moneymanager.goal.SavingsGoalAmountException;
import com.maahfuzdev.moneymanager.goal.SavingsGoalNotFoundException;
import com.maahfuzdev.moneymanager.transaction.TransactionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Please check the submitted fields.", request, fieldErrors);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> duplicateEmail(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "An account with this email already exists.", request, Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.", request, Map.of());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    ResponseEntity<ApiError> transactionNotFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Transaction not found.", request, Map.of());
    }

    @ExceptionHandler(BudgetNotFoundException.class)
    ResponseEntity<ApiError> budgetNotFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Budget not found.", request, Map.of());
    }

    @ExceptionHandler(BudgetAlreadyExistsException.class)
    ResponseEntity<ApiError> budgetAlreadyExists(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "A budget already exists for this category and month.", request, Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Please check the submitted fields.", request, Map.of());
    }

    @ExceptionHandler(SavingsGoalNotFoundException.class)
    ResponseEntity<ApiError> savingsGoalNotFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Savings goal not found.", request, Map.of());
    }

    @ExceptionHandler(SavingsGoalAmountException.class)
    ResponseEntity<ApiError> savingsGoalAmount(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Current savings cannot be greater than the target amount.", request, Map.of());
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
