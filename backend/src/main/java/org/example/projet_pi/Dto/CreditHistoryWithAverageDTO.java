package org.example.projet_pi.Dto;

import org.example.projet_pi.Dto.CreditHistoryDTO;

import java.util.List;

public class CreditHistoryWithAverageDTO {

    private List<CreditHistoryDTO> credits;
    private double averageLatePercentage;

    public CreditHistoryWithAverageDTO(List<CreditHistoryDTO> credits, double averageLatePercentage) {
        this.credits = credits;
        this.averageLatePercentage = averageLatePercentage;
    }

    public List<CreditHistoryDTO> getCredits() {
        return credits;
    }

    public void setCredits(List<CreditHistoryDTO> credits) {
        this.credits = credits;
    }

    public double getAverageLatePercentage() {
        return averageLatePercentage;
    }

    public void setAverageLatePercentage(double averageLatePercentage) {
        this.averageLatePercentage = averageLatePercentage;
    }
}