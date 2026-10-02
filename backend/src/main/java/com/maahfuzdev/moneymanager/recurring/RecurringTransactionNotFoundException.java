package com.maahfuzdev.moneymanager.recurring;

public class RecurringTransactionNotFoundException extends RuntimeException {
    public RecurringTransactionNotFoundException() { super("Recurring transaction not found"); }
}
