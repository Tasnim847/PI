package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class ClaimMapper {

    // Entity -> DTO
    public static ClaimDTO toDTO(Claim claim) {
        if (claim == null) return null;

        ClaimDTO dto = new ClaimDTO();
        dto.setClaimId(claim.getClaimId());
        dto.setClaimDate(claim.getClaimDate());
        dto.setClaimedAmount(claim.getClaimedAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setDescription(claim.getDescription());
        dto.setStatus(claim.getStatus() != null ? claim.getStatus().name() : null);

        if (claim.getContract() != null) {
            dto.setContractId(claim.getContract().getContractId());
        }
        if (claim.getCompensation() != null) {
            dto.setCompensationId(claim.getCompensation().getCompensationId());
        }
        if (claim.getRiskClaim() != null) {
            dto.setRiskClaimId(claim.getRiskClaim().getRiskId()); // corrigé ici
        }

        if (claim.getDocuments() != null) {
            dto.setDocumentIds(
                    claim.getDocuments()
                            .stream()
                            .map(Document::getDocumentId)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    // DTO -> Entity
    public static Claim toEntity(ClaimDTO dto,
                                 InsuranceContract contract,
                                 Compensation compensation,
                                 RiskClaim riskClaim,
                                 List<Document> documents) {
        if (dto == null) return null;

        Claim claim = new Claim();
        claim.setClaimId(dto.getClaimId());
        claim.setClaimDate(dto.getClaimDate());
        claim.setClaimedAmount(dto.getClaimedAmount());
        claim.setApprovedAmount(dto.getApprovedAmount());
        claim.setDescription(dto.getDescription());
        claim.setContract(contract);
        claim.setCompensation(compensation);
        claim.setRiskClaim(riskClaim);
        claim.setDocuments(documents);

        return claim;
    }

}