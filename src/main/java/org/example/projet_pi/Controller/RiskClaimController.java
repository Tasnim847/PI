package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.RiskClaimDTO;
import org.example.projet_pi.Service.IRiskClaimService;
import org.example.projet_pi.entity.RiskClaim;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/riskclaims")
public class RiskClaimController {

    private IRiskClaimService riskClaimService;

    @PostMapping("/addRiskClaim")
    public RiskClaimDTO addRiskClaim(@RequestBody RiskClaimDTO dto) {
        return riskClaimService.addRiskClaim(dto);
    }

    @PutMapping("/updateRiskClaim")
    public RiskClaimDTO updateRiskClaim(@RequestBody RiskClaimDTO dto) {
        return riskClaimService.updateRiskClaim(dto);
    }

    @DeleteMapping("/deleteRiskClaim/{id}")
    public void deleteRiskClaim(@PathVariable Long id) {
        riskClaimService.deleteRiskClaim(id);
    }

    @GetMapping("/getRiskClaim/{id}")
    public RiskClaimDTO getRiskClaimById(@PathVariable Long id) {
        return riskClaimService.getRiskClaimById(id);
    }

    @GetMapping("/allRiskClaims")
    public List<RiskClaimDTO> getAllRiskClaims() {
        return riskClaimService.getAllRiskClaims();
    }
}