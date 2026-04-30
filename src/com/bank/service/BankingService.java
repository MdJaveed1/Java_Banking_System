package com.bank.service;

import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.User;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BankingService {
    private Map<String, User> users; // username -> User
    private Map<String, Account> accounts; // accountNumber -> Account
    private AtomicInteger accountNumberGenerator;

    public BankingService() {
        this.users = new ConcurrentHashMap<>();
        this.accounts = new ConcurrentHashMap<>();
        this.accountNumberGenerator = new AtomicInteger(1000);
    }

    public void registerUser(String username, String password) throws Exception {
        if (users.containsKey(username)) {
            throw new Exception("User already exists.");
        }
        users.put(username, new User(username, password));
    }

    public User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.authenticate(password)) {
            return user;
        }
        return null;
    }

    public Account createAccount(String username, String accountType, double initialDeposit) throws Exception {
        User user = users.get(username);
        if (user == null) throw new Exception("User not found.");

        String accountNumber = "ACC" + accountNumberGenerator.incrementAndGet();
        Account newAccount;

        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            newAccount = new SavingsAccount(accountNumber, initialDeposit, 0.05); // 5% interest
        } else if ("CURRENT".equalsIgnoreCase(accountType)) {
            newAccount = new CurrentAccount(accountNumber, initialDeposit, 500.0); // 500 overdraft
        } else {
            throw new Exception("Invalid account type.");
        }

        user.addAccount(newAccount);
        accounts.put(accountNumber, newAccount);
        return newAccount;
    }

    public void deposit(String accountNumber, double amount) throws Exception {
        Account account = getAccount(accountNumber);
        account.deposit(amount);
    }

    public void withdraw(String accountNumber, double amount) throws Exception {
        Account account = getAccount(accountNumber);
        account.withdraw(amount);
    }

    public void transfer(String fromAccountNum, String toAccountNum, double amount) throws Exception {
        Account fromAccount = getAccount(fromAccountNum);
        Account toAccount = getAccount(toAccountNum);
        fromAccount.transfer(toAccount, amount);
    }

    public Account getAccount(String accountNumber) throws Exception {
        Account account = accounts.get(accountNumber);
        if (account == null) throw new Exception("Account not found.");
        return account;
    }
}
