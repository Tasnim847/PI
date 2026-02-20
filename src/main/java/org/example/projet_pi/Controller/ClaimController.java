package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IClaimService;
import org.example.projet_pi.Dto.ClaimDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/claims")
public class ClaimController {

    private final IClaimService claimService;

    // Ajouter un Claim
    @PostMapping("/add")
    public ClaimDTO addClaim(@RequestBody ClaimDTO claimDTO) {
        return claimService.addClaim(claimDTO);
    }

    // Mettre à jour un Claim
    @PutMapping("/update")
    public ClaimDTO updateClaim(@RequestBody ClaimDTO claimDTO) {
        return claimService.updateClaim(claimDTO);
    }

    // Supprimer un Claim
    @DeleteMapping("/delete/{id}")
    public void deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
    }

    // Récupérer un Claim par id
    @GetMapping("/getClaim/{id}")
    public ClaimDTO getClaimById(@PathVariable Long id) {
        return claimService.getClaimById(id);
    }

    // Récupérer tous les Claims
    @GetMapping("/allClaim")
    public List<ClaimDTO> getAllClaims() {
        return claimService.getAllClaims();
    }
}