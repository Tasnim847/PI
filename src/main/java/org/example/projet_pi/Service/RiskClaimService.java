package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.RiskClaimRepository;
import org.example.projet_pi.entity.RiskClaim;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RiskClaimService implements IRiskClaimService {

    RiskClaimRepository riskClaimRepository;

    @Override
    public RiskClaim addRiskClaim(RiskClaim riskClaim) {
        return riskClaimRepository.save(riskClaim);
    }

    @Override
    public RiskClaim updateRiskClaim(RiskClaim riskClaim) {
        return riskClaimRepository.save(riskClaim);
    }

    @Override
    public void deleteRiskClaim(Long id) {
        riskClaimRepository.deleteById(id);
    }

    @Override
    public RiskClaim getRiskClaimById(Long id) {
        return riskClaimRepository.findById(id).get();
    }

    @Override
    public List<RiskClaim> getAllRiskClaims() {
        return riskClaimRepository.findAll();
    }
}
