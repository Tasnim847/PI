package org.example.projet_pi.Mapper;


import org.example.projet_pi.Dto.CompensationDTO;
import org.example.projet_pi.entity.Compensation;
import org.example.projet_pi.entity.Claim;

public class CompensationMapper {

    // Entity -> DTO
    public static CompensationDTO toDTO(Compensation compensation) {
        if (compensation == null) return null;

        CompensationDTO dto = new CompensationDTO();
        dto.setCompensationId(compensation.getCompensationId());
        dto.setAmount(compensation.getAmount());
        dto.setPaymentDate(compensation.getPaymentDate());

        if (compensation.getClaim() != null) {
            dto.setClaimId(compensation.getClaim().getClaimId());
        }

        return dto;
    }

    // DTO -> Entity
    public static Compensation toEntity(CompensationDTO dto, Claim claim) {
        if (dto == null) return null;

        Compensation compensation = new Compensation();
        compensation.setCompensationId(dto.getCompensationId());
        compensation.setAmount(dto.getAmount());
        compensation.setPaymentDate(dto.getPaymentDate());
        compensation.setClaim(claim);

        return compensation;
    }
}
