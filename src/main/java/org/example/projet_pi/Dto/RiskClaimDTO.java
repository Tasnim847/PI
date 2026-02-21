package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiskClaimDTO {

    private Long riskId;

    private double riskScore;
    private String riskLevel;
    private String evaluationNote;

    private Long contractId;   // 🔥 on expose seulement l'id du contrat
}