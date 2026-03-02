package org.example.projet_pi.Dto;

public class AccountStatisticsDTO {

    private Long accountId;
    private double totalDeposits;
    private double totalWithdrawals;
    private double currentBalance;
    private double averageTransactionAmount;
    private long totalTransactions;
    private long totalDepositCount;
    private long totalWithdrawalCount;

    // Constructeur
    public AccountStatisticsDTO(Long accountId,
                                double totalDeposits,
                                double totalWithdrawals,
                                double currentBalance,
                                double averageTransactionAmount,
                                long totalTransactions,
                                long totalDepositCount,
                                long totalWithdrawalCount) {
        this.accountId = accountId;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.currentBalance = currentBalance;
        this.averageTransactionAmount = averageTransactionAmount;
        this.totalTransactions = totalTransactions;
        this.totalDepositCount = totalDepositCount;
        this.totalWithdrawalCount = totalWithdrawalCount;
    }

    // Getters
    public Long getAccountId() { return accountId; }
    public double getTotalDeposits() { return totalDeposits; }
    public double getTotalWithdrawals() { return totalWithdrawals; }
    public double getCurrentBalance() { return currentBalance; }
    public double getAverageTransactionAmount() { return averageTransactionAmount; }
    public long getTotalTransactions() { return totalTransactions; }
    public long getTotalDepositCount() { return totalDepositCount; }
    public long getTotalWithdrawalCount() { return totalWithdrawalCount; }
}