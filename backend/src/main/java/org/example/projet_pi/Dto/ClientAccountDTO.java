package org.example.projet_pi.Dto;

import org.example.projet_pi.entity.AccountType;
import java.time.LocalDateTime;

public class ClientAccountDTO {
    private Long accountId;
    private String rip;
    private double balance;
    private AccountType type;
    private String status;
    private LocalDateTime createdAt;
    private double dailyLimit;
    private double monthlyLimit;
    private double dailyTransferLimit;

    public ClientAccountDTO(Long accountId, String rip, double balance,
                            AccountType type, String status, LocalDateTime createdAt,
                            double dailyLimit, double monthlyLimit, double dailyTransferLimit) {
        this.accountId = accountId;
        this.rip = rip;
        this.balance = balance;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.dailyTransferLimit = dailyTransferLimit;
    }

    // Getters
    public Long getAccountId() { return accountId; }
    public String getRip() { return rip; }
    public double getBalance() { return balance; }
    public AccountType getType() { return type; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public double getDailyLimit() { return dailyLimit; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public double getDailyTransferLimit() { return dailyTransferLimit; }
}