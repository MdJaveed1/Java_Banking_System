package com.bank.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Account {
    private String accountNumber;
    protected double balance;
    private List<Transaction> transactions;

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
        if (initialBalance > 0) {
            this.transactions.add(new Transaction("DEPOSIT", initialBalance, "Initial Deposit"));
        }
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return transactions; }

    public abstract String getAccountType();

    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        this.balance += amount;
        this.transactions.add(new Transaction("DEPOSIT", amount, "Cash Deposit"));
    }

    public synchronized void withdraw(double amount) throws Exception {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if (this.balance < amount) throw new Exception("Insufficient funds.");
        this.balance -= amount;
        this.transactions.add(new Transaction("WITHDRAWAL", amount, "Cash Withdrawal"));
    }

    public synchronized void transfer(Account targetAccount, double amount) throws Exception {
        if (amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive.");
        if (this.balance < amount) throw new Exception("Insufficient funds.");
        
        this.balance -= amount;
        this.transactions.add(new Transaction("TRANSFER_OUT", amount, "Transfer to " + targetAccount.getAccountNumber()));
        
        targetAccount.receiveTransfer(this.accountNumber, amount);
    }

    protected synchronized void receiveTransfer(String fromAccount, double amount) {
        this.balance += amount;
        this.transactions.add(new Transaction("TRANSFER_IN", amount, "Transfer from " + fromAccount));
    }
}
