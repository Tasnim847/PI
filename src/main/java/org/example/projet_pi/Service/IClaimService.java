package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ClaimDTO;

import java.util.List;


public interface IClaimService {

    ClaimDTO addClaim(ClaimDTO claimDTO);

    ClaimDTO updateClaim(ClaimDTO claimDTO);

    void deleteClaim(Long id);

    ClaimDTO getClaimById(Long id);

    List<ClaimDTO> getAllClaims();
}