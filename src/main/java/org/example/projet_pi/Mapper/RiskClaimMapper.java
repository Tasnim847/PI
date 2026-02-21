package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.RiskClaimDTO;
import org.example.projet_pi.entity.RiskClaim;

public class RiskClaimMapper {

    // Entity -> DTO
    public static RiskClaimDTO toDTO(RiskClaim riskClaim) {
        if (riskClaim == null) return null;

        RiskClaimDTO dto = new RiskClaimDTO();
        dto.setRiskId(riskClaim.getRiskId());
        dto.setRiskScore(riskClaim.getRiskScore());
        dto.setRiskLevel(riskClaim.getRiskLevel());
        dto.setEvaluationNote(riskClaim.getEvaluationNote());

        if (riskClaim.getContract() != null) {
            dto.setContractId(riskClaim.getContract().getContractId());
        }

        return dto;
    }

    // DTO -> Entity
    public static RiskClaim toEntity(RiskClaimDTO dto) {
        if (dto == null) return null;

        RiskClaim riskClaim = new RiskClaim();
        riskClaim.setRiskId(dto.getRiskId());
        riskClaim.setRiskScore(dto.getRiskScore());
        riskClaim.setRiskLevel(dto.getRiskLevel());
        riskClaim.setEvaluationNote(dto.getEvaluationNote());

        return riskClaim;
    }
}