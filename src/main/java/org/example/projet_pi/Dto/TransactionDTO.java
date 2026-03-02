package org.example.projet_pi.Dto;

import java.time.LocalDate;

public class TransactionDTO {

    private Long transactionId;
    private double amount;
    private String type;
    private LocalDate date;

    // Constructeur
    public TransactionDTO(Long transactionId, double amount, String type, LocalDate date) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.type = type;
        this.date = date;
    }

    // Getters
    public Long getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public LocalDate getDate() { return date; }
}