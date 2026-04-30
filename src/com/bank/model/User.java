package com.bank.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password; // In a real app, hash this!
    private List<Account> accounts;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.accounts = new ArrayList<>();
    }

    public String getUsername() { return username; }
    
    public boolean authenticate(String password) {
        return this.password.equals(password);
    }

    public List<Account> getAccounts() { return accounts; }

    public void addAccount(Account account) {
        this.accounts.add(account);
    }

    public Account getAccount(String accountNumber) {
        for (Account acc : accounts) {
            if (acc.getAccountNumber().equals(accountNumber)) {
                return acc;
            }
        }
        return null;
    }
}
