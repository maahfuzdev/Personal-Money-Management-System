package com.maahfuzdev.moneymanager.transaction;

public class InvalidTransactionDateRangeException extends RuntimeException {
    public InvalidTransactionDateRangeException() { super("Start date must be on or before end date."); }
}
