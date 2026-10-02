package com.maahfuzdev.moneymanager.transaction;

public class TransactionExportTooLargeException extends RuntimeException {
    public TransactionExportTooLargeException() {
        super("This export exceeds the 10,000 transaction limit. Narrow your filters and try again.");
    }
}
