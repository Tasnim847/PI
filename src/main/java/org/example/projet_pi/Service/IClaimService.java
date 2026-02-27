package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.CompensationDetailsDTO;
import java.util.List;

public interface IClaimService {

    ClaimDTO addClaim(ClaimDTO claimDTO, String userEmail);

    ClaimDTO updateClaim(ClaimDTO claimDTO, String userEmail);

    void deleteClaim(Long id, String userEmail);

    ClaimDTO getClaimById(Long id, String userEmail);

    List<ClaimDTO> getAllClaims(String userEmail);

    ClaimDTO approveClaim(Long claimId, Double approvedAmount, String userEmail);

    ClaimDTO rejectClaim(Long claimId, String reason, String userEmail);

    CompensationDetailsDTO getCompensationDetails(Long claimId);
}