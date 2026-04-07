package org.example.projet_pi.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimScoreDTO {
    private Long claimId;
    private Double claimedAmount;
    private int riskScore;
    private String riskLevel;
    private String recommendation;
    private String colorCode;
    private String delayInfo;
    private String documentTypeInfo;
    private String frequencyInfo;

    // Nouveaux champs pour la décision
    private boolean isSuspicious;
    private DecisionSuggestion decisionSuggestion;

    public enum DecisionSuggestion {
        AUTO_APPROVE,
        AUTO_REJECT,
        MANUAL_REVIEW
    }
}