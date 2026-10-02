package com.maahfuzdev.moneymanager.budget;

public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException() { super("Budget not found."); }
}
