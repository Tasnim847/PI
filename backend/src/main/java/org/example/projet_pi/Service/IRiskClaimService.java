package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.RiskClaimDTO;

import java.util.List;

public interface IRiskClaimService {

    RiskClaimDTO addRiskClaim(RiskClaimDTO dto);

    RiskClaimDTO updateRiskClaim(RiskClaimDTO dto);

    void deleteRiskClaim(Long id);

    RiskClaimDTO getRiskClaimById(Long id);

    List<RiskClaimDTO> getAllRiskClaims();
}