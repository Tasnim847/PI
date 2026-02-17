package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IRiskClaimService;
import org.example.projet_pi.entity.RiskClaim;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/riskclaims")
public class RiskClaimController {

    IRiskClaimService riskClaimService;

    @PostMapping("/addRiskClaim")
    public RiskClaim addRiskClaim(@RequestBody RiskClaim riskClaim) {
        RiskClaim newRiskClaim = riskClaimService.addRiskClaim(riskClaim);
        return newRiskClaim;
    }

    @PutMapping("/updateRiskClaim")
    public RiskClaim updateRiskClaim(@RequestBody RiskClaim riskClaim) {
        RiskClaim newRiskClaim = riskClaimService.updateRiskClaim(riskClaim);
        return newRiskClaim;
    }

    @DeleteMapping("/deleteRiskClaim/{id}")
    public void deleteRiskClaim(@PathVariable Long id) {
        riskClaimService.deleteRiskClaim(id);
    }

    @GetMapping("/getRiskClaim/{id}")
    public RiskClaim getRiskClaimById(@PathVariable Long id) {
        RiskClaim riskClaim = riskClaimService.getRiskClaimById(id);
        return riskClaim;
    }

    @GetMapping("/allRiskClaims")
    public List<RiskClaim> getAllRiskClaims() {
        List<RiskClaim> riskClaims = riskClaimService.getAllRiskClaims();
        return riskClaims;
    }
}
