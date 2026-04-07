package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.CompensationDTO;
import org.example.projet_pi.entity.Compensation;
import org.example.projet_pi.entity.Claim;

public class CompensationMapper {

    // Entity -> DTO (complet)
    public static CompensationDTO toDTO(Compensation compensation) {
        if (compensation == null) return null;

        CompensationDTO dto = new CompensationDTO();
        dto.setCompensationId(compensation.getCompensationId());
        dto.setAmount(compensation.getAmount());
        dto.setPaymentDate(compensation.getPaymentDate());
        dto.setClientOutOfPocket(compensation.getClientOutOfPocket());
        dto.setCoverageLimit(compensation.getCoverageLimit());
        dto.setDeductible(compensation.getDeductible());
        dto.setOriginalClaimedAmount(compensation.getOriginalClaimedAmount());
        dto.setApprovedAmount(compensation.getApprovedAmount());
        dto.setMessage(compensation.getMessage());
        dto.setStatus(compensation.getStatus());
        dto.setCalculationDate(compensation.getCalculationDate());

        if (compensation.getClaim() != null) {
            dto.setClaimId(compensation.getClaim().getClaimId());
        }

        return dto;
    }

    // DTO -> Entity (complet)
    public static Compensation toEntity(CompensationDTO dto, Claim claim) {
        if (dto == null) return null;

        Compensation compensation = new Compensation();
        compensation.setCompensationId(dto.getCompensationId());
        compensation.setAmount(dto.getAmount());
        compensation.setPaymentDate(dto.getPaymentDate());
        compensation.setClientOutOfPocket(dto.getClientOutOfPocket());
        compensation.setCoverageLimit(dto.getCoverageLimit());
        compensation.setDeductible(dto.getDeductible());
        compensation.setOriginalClaimedAmount(dto.getOriginalClaimedAmount());
        compensation.setApprovedAmount(dto.getApprovedAmount());
        compensation.setMessage(dto.getMessage());
        compensation.setStatus(dto.getStatus());
        compensation.setCalculationDate(dto.getCalculationDate());
        compensation.setClaim(claim);

        return compensation;
    }

    // Version simplifiée pour la rétrocompatibilité
    public static Compensation toEntitySimple(CompensationDTO dto, Claim claim) {
        if (dto == null) return null;

        Compensation compensation = new Compensation();
        compensation.setCompensationId(dto.getCompensationId());
        compensation.setAmount(dto.getAmount());
        compensation.setPaymentDate(dto.getPaymentDate());
        compensation.setClaim(claim);

        return compensation;
    }
}