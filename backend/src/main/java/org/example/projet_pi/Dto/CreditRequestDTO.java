package org.example.projet_pi.Dto;

import java.time.LocalDate;

public class CreditRequestDTO {
    private double amount;
    private int durationInMonths;
    private LocalDate dueDate;

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getDurationInMonths() { return durationInMonths; }
    public void setDurationInMonths(int durationInMonths) { this.durationInMonths = durationInMonths; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}