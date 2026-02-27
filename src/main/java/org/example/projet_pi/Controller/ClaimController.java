package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.ClaimService;
import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.CompensationDetailsDTO;  // ← AJOUTEZ CET IMPORT
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping("/addClaim")
    public ClaimDTO addClaim(
            @RequestBody ClaimDTO claimDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.addClaim(claimDTO, currentUser.getUsername());
    }

    @PutMapping("/updateClaim")
    public ClaimDTO updateClaim(
            @RequestBody ClaimDTO claimDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.updateClaim(claimDTO, currentUser.getUsername());
    }

    @DeleteMapping("/deleteClaim/{id}")
    public void deleteClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        claimService.deleteClaim(id, currentUser.getUsername());
    }

    @GetMapping("/getClaim/{id}")
    public ClaimDTO getClaimById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.getClaimById(id, currentUser.getUsername());
    }

    @GetMapping("/allClaim")
    public List<ClaimDTO> getAllClaims(
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.getAllClaims(currentUser.getUsername());
    }

    @PostMapping("/approve/{id}")
    public ClaimDTO approveClaim(
            @PathVariable Long id,
            @RequestParam(required = false) Double approvedAmount,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.approveClaim(id, approvedAmount, currentUser.getUsername());
    }

    @PostMapping("/reject/{id}")
    public ClaimDTO rejectClaim(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.rejectClaim(id, reason, currentUser.getUsername());
    }

    // ✅ NOUVEAU ENDPOINT pour obtenir les détails de compensation
    @GetMapping("/calculate-compensation/{id}")
    public CompensationDetailsDTO calculateCompensation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        // Note: Vous pouvez ajouter une vérification des droits si nécessaire
        return claimService.getCompensationDetails(id);
    }

    // ✅ Version texte pour faciliter la lecture
    @GetMapping("/calculate-compensation/{id}/text")
    public String calculateCompensationText(@PathVariable Long id) {
        CompensationDetailsDTO details = claimService.getCompensationDetails(id);
        return String.format(
                "Claim %d: %s\n" +
                        "   Montant réclamé: %.2f DT\n" +
                        "   Montant approuvé: %.2f DT\n" +
                        "   Franchise client: %.2f DT\n" +
                        "   Montant assurance: %.2f DT",
                details.getClaimId(),
                details.getStatus(),
                details.getClaimedAmount(),
                details.getApprovedAmount(),
                details.getFranchise(),
                details.getInsurancePayment()
        );
    }
}