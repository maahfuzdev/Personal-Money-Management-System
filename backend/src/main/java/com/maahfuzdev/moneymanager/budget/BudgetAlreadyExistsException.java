package com.maahfuzdev.moneymanager.budget;

public class BudgetAlreadyExistsException extends RuntimeException {
    public BudgetAlreadyExistsException() { super("A budget already exists for this category and month."); }
}
