package com.bank.model;

import java.time.LocalDateTime;

public class Transaction {
    private String type; // "DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT"
    private double amount;
    private LocalDateTime date;
    private String description;

    public Transaction(String type, double amount, String description) {
        this.type = type;
        this.amount = amount;
        this.date = LocalDateTime.now();
        this.description = description;
    }

    public String getType() { return type; }
    public double getAmount() { return amount; }
    public LocalDateTime getDate() { return date; }
    public String getDescription() { return description; }
}
