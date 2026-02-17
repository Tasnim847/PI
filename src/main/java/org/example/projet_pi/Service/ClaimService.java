package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.entity.Claim;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ClaimService implements IClaimService {

    ClaimRepository claimRepository;

    //service

    @Override
    public Claim addClaim(Claim claim) {
        return claimRepository.save(claim);
    }

    @Override
    public Claim updateClaim(Claim claim) {
        return claimRepository.save(claim);
    }

    @Override
    public void deleteClaim(Long id) {
        claimRepository.deleteById(id);
    }

    @Override
    public Claim getClaimById(Long id) {
        return claimRepository.findById(id).get();
    }

    @Override
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }
}
