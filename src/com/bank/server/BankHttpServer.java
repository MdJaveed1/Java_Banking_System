package com.bank.server;

import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.BankingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class BankHttpServer {
    private BankingService bankingService;
    private HttpServer server;

    public BankHttpServer(BankingService bankingService, int port) throws IOException {
        this.bankingService = bankingService;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    private void setupRoutes() {
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/accounts", new AccountsHandler());
        server.createContext("/api/transaction", new TransactionHandler());
        server.createContext("/api/createAccount", new CreateAccountHandler());
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    map.put(entry[0], entry[1]);
                } else {
                    map.put(entry[0], "");
                }
            }
        }
        return map;
    }

    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            File file = new File("public" + path);
            if (file.exists() && !file.isDirectory()) {
                if (path.endsWith(".html")) exchange.getResponseHeaders().set("Content-Type", "text/html");
                else if (path.endsWith(".css")) exchange.getResponseHeaders().set("Content-Type", "text/css");
                else if (path.endsWith(".js")) exchange.getResponseHeaders().set("Content-Type", "application/javascript");

                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                // simple custom JSON parse: {"username":"a","password":"b"}
                String username = extractJsonValue(body, "username");
                String password = extractJsonValue(body, "password");

                User user = bankingService.authenticateUser(username, password);
                if (user != null) {
                    sendResponse(exchange, 200, "{\"status\":\"success\", \"message\":\"Login successful\"}");
                } else {
                    sendResponse(exchange, 401, "{\"status\":\"error\", \"message\":\"Invalid credentials\"}");
                }
            }
        }
    }

    class AccountsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                String username = params.get("username");
                if (username != null) {
                    try {
                        // Normally auth via session, but we do simple parameter check for demo
                        User user = bankingService.authenticateUser(username, params.get("password"));
                        if (user == null) {
                            sendResponse(exchange, 401, "{\"status\":\"error\", \"message\":\"Unauthorized\"}");
                            return;
                        }
                        
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < user.getAccounts().size(); i++) {
                            Account acc = user.getAccounts().get(i);
                            sb.append("{")
                              .append("\"accountNumber\":\"").append(acc.getAccountNumber()).append("\",")
                              .append("\"type\":\"").append(acc.getAccountType()).append("\",")
                              .append("\"balance\":").append(acc.getBalance()).append(",")
                              .append("\"transactions\": [");
                            
                            for(int j = 0; j < acc.getTransactions().size(); j++) {
                                Transaction t = acc.getTransactions().get(j);
                                sb.append("{")
                                  .append("\"type\":\"").append(t.getType()).append("\",")
                                  .append("\"amount\":").append(t.getAmount()).append(",")
                                  .append("\"date\":\"").append(t.getDate().toString()).append("\",")
                                  .append("\"desc\":\"").append(t.getDescription()).append("\"}");
                                if (j < acc.getTransactions().size() - 1) sb.append(",");
                            }
                            sb.append("]}");
                            if (i < user.getAccounts().size() - 1) sb.append(",");
                        }
                        sb.append("]");
                        sendResponse(exchange, 200, sb.toString());
                    } catch (Exception e) {
                        sendResponse(exchange, 400, "{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
                    }
                }
            }
        }
    }

    class TransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    
                    String type = extractJsonValue(body, "type"); // deposit, withdraw, transfer
                    String accountNum = extractJsonValue(body, "accountNumber");
                    double amount = Double.parseDouble(extractJsonValue(body, "amount"));
                    
                    if ("deposit".equals(type)) {
                        bankingService.deposit(accountNum, amount);
                    } else if ("withdraw".equals(type)) {
                        bankingService.withdraw(accountNum, amount);
                    } else if ("transfer".equals(type)) {
                        String targetNum = extractJsonValue(body, "targetNumber");
                        bankingService.transfer(accountNum, targetNum, amount);
                    }
                    sendResponse(exchange, 200, "{\"status\":\"success\", \"message\":\"Transaction successful\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    class CreateAccountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    String username = extractJsonValue(body, "username");
                    String type = extractJsonValue(body, "type");
                    double initialDeposit = Double.parseDouble(extractJsonValue(body, "initialDeposit"));

                    bankingService.createAccount(username, type, initialDeposit);
                    sendResponse(exchange, 200, "{\"status\":\"success\", \"message\":\"Account created\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    // Very naive JSON parser helper for demo purposes
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        start += searchKey.length();
        
        // Skip spaces
        while(json.charAt(start) == ' ' || json.charAt(start) == '\"') start++;
        
        int end = start;
        while(end < json.length() && json.charAt(end) != '\"' && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }
}
