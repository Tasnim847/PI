package org.example.projet_pi.Service;

import org.example.projet_pi.entity.RiskClaim;

import java.util.List;

public interface IRiskClaimService {

    RiskClaim addRiskClaim(RiskClaim riskClaim);

    RiskClaim updateRiskClaim(RiskClaim riskClaim);

    void deleteRiskClaim(Long id);

    RiskClaim getRiskClaimById(Long id);

    List<RiskClaim> getAllRiskClaims();
}
