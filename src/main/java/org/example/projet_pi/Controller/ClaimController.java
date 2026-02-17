package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IClaimService;
import org.example.projet_pi.entity.Claim;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/claims")
public class ClaimController {
    IClaimService claimService;

    //crude
    @PostMapping("/addClaim")
    public Claim addClaim(@RequestBody Claim claim) {
        Claim claim1 = claimService.addClaim(claim);
        return claim1;
    }

    @PutMapping("/updateClaim")
    public Claim updateClaim(@RequestBody Claim claim) {
        Claim claim1 = claimService.updateClaim(claim);
        return claim1;
    }

    @DeleteMapping("/deleteClaim/{id}")
    public void deleteClaim(@PathVariable("id") Long id) {
        claimService.deleteClaim(id);
    }

    @GetMapping("/getClaim/{id}")
    public Claim getClaimById(@PathVariable("id") Long id) {
        Claim claim = claimService.getClaimById(id);
        return claim;
    }

    @GetMapping("/allClaim")
    public List<Claim> getAllClaims() {
        List<Claim> claims = claimService.getAllClaims();
        return claims;
    }
}
