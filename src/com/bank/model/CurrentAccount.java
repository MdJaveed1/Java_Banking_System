package com.bank.model;

public class CurrentAccount extends Account {
    private double overdraftLimit;

    public CurrentAccount(String accountNumber, double initialBalance, double overdraftLimit) {
        super(accountNumber, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public String getAccountType() {
        return "Current";
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public synchronized void withdraw(double amount) throws Exception {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if (getBalance() + overdraftLimit < amount) {
            throw new Exception("Overdraft limit exceeded.");
        }
        this.balance -= amount;
        getTransactions().add(new Transaction("WITHDRAWAL", amount, "Cash Withdrawal"));
    }
}
