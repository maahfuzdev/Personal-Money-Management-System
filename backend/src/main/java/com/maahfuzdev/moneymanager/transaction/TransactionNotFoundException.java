package com.maahfuzdev.moneymanager.transaction;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transaction not found.");
    }
}
