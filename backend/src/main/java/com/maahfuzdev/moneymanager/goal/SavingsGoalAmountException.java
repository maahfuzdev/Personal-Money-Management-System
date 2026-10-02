package com.maahfuzdev.moneymanager.goal;

public class SavingsGoalAmountException extends RuntimeException {
    public SavingsGoalAmountException() { super("Current savings cannot be greater than the target amount."); }
}
