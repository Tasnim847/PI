package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IClaimService;
import org.example.projet_pi.Service.ClaimService;
import org.example.projet_pi.Dto.ClaimDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/claims")
public class ClaimController {

    private final IClaimService claimService;
    private final ClaimService concreteClaimService; // Injection du service concret

    // ========== CRUD ==========

    @PostMapping("/addClaim")
    public ClaimDTO addClaim(@RequestBody ClaimDTO claimDTO) {
        return claimService.addClaim(claimDTO);
    }

    @PutMapping("/updateClaim")
    public ClaimDTO updateClaim(@RequestBody ClaimDTO claimDTO) {
        return claimService.updateClaim(claimDTO);
    }

    @DeleteMapping("/deleteClaim/{id}")
    public void deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
    }

    @GetMapping("/getClaim/{id}")
    public ClaimDTO getClaimById(@PathVariable Long id) {
        return claimService.getClaimById(id);
    }

    @GetMapping("/allClaim")
    public List<ClaimDTO> getAllClaims() {
        return claimService.getAllClaims();
    }

    // ========== GESTION DES CLAIMS ==========

    /**
     * Approuver un claim (crée automatiquement la compensation)
     * @param id ID du claim
     * @param approvedAmount Montant approuvé (optionnel)
     */
    @PostMapping("/approve/{id}")
    public ClaimDTO approveClaim(
            @PathVariable Long id,
            @RequestParam(required = false) Double approvedAmount) {
        return concreteClaimService.approveClaim(id, approvedAmount);
    }

    /**
     * Rejeter un claim
     * @param id ID du claim
     * @param reason Raison du rejet
     */
    @PostMapping("/reject/{id}")
    public ClaimDTO rejectClaim(
            @PathVariable Long id,
            @RequestParam String reason) {
        return concreteClaimService.rejectClaim(id, reason);
    }

    /**
     * Obtenir les détails de compensation potentielle
     * @param id ID du claim
     */
    @GetMapping("/calculate-compensation/{id}")
    public ClaimService.CompensationDetails calculateCompensation(@PathVariable Long id) {
        return concreteClaimService.getCompensationDetails(id);
    }

    /**
     * Version texte des détails de compensation
     */
    @GetMapping("/calculate-compensation/{id}/text")
    public String calculateCompensationText(@PathVariable Long id) {
        ClaimService.CompensationDetails details =
                concreteClaimService.getCompensationDetails(id);
        return details.toString();
    }
}