package org.example.projet_pi.Dto;

import org.example.projet_pi.entity.Credit;



public class CreditHistoryDTO {
    private Credit credit;
    private double lateRepaymentPercentage;

    public CreditHistoryDTO(Credit credit, double lateRepaymentPercentage) {
        this.credit = credit;
        this.lateRepaymentPercentage = lateRepaymentPercentage;
    }

    // Getters/Setters

    public Credit getCredit() {
        return credit;
    }

    public void setCredit(Credit credit) {
        this.credit = credit;
    }

    public double getLateRepaymentPercentage() {
        return lateRepaymentPercentage;
    }

    public void setLateRepaymentPercentage(double lateRepaymentPercentage) {
        this.lateRepaymentPercentage = lateRepaymentPercentage;
    }
}
