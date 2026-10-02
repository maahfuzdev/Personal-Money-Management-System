package com.maahfuzdev.moneymanager.transaction;

public class InvalidTransactionCsvException extends RuntimeException {
    public InvalidTransactionCsvException(String message) { super(message); }
}
