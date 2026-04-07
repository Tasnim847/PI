package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiskClaimDTO {

    private Long riskId;

    private Double riskScore;
    private String riskLevel;
    private String evaluationNote;

    private Long contractId;   // 🔥 on expose seulement l'id du contrat
}