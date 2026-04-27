package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditScoreDTO {
    
    private Long clientId;
    private String clientName;
    private String clientEmail;
    
    // Score et évaluation
    private Integer score;
    private String riskLevel;
    private String recommendation;
    private String analysis;
    private String calculatedAt;
    
    // Métriques
    private Integer totalCredits;
    private Integer activeCredits;
    private Integer closedCredits;
    private BigDecimal totalAmount;
    private BigDecimal currentDebt;
    private Double averageLatePercentage;
    private Double averageMonthlyPayment;
    private Long daysSinceLastCredit;
    
    // Constructeur minimal
    public CreditScoreDTO(Long clientId, String clientName, String clientEmail) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.calculatedAt = LocalDateTime.now().toString();
    }
}
