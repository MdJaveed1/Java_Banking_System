package com.bank;

import com.bank.service.BankingService;
import com.bank.server.BankHttpServer;

public class Main {
    public static void main(String[] args) {
        try {
            BankingService bankingService = new BankingService();
            
            // Create some mock data
            bankingService.registerUser("user", "password");
            bankingService.createAccount("user", "SAVINGS", 1500.00);
            bankingService.createAccount("user", "CURRENT", 500.00);

            // Start server on port 8081
            BankHttpServer server = new BankHttpServer(bankingService, 8081);
            server.start();
            System.out.println("Visit http://localhost:8081 in your browser.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
