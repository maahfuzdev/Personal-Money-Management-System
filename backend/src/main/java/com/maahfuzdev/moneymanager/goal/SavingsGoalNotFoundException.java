package com.maahfuzdev.moneymanager.goal;

public class SavingsGoalNotFoundException extends RuntimeException {
    public SavingsGoalNotFoundException() { super("Savings goal not found."); }
}
