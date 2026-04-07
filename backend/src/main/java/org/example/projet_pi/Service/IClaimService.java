package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.CompensationDetailsDTO;

import java.util.List;
import java.util.Map;

public interface IClaimService {

    // ===============================
    // BASIC CRUD
    // ===============================
    ClaimDTO addClaim(ClaimDTO claimDTO, String userEmail);

    ClaimDTO updateClaim(ClaimDTO claimDTO, String userEmail);

    void deleteClaim(Long id, String userEmail);

    ClaimDTO getClaimById(Long id, String userEmail);

    List<ClaimDTO> getAllClaims(String userEmail);

    // ===============================
    // CLAIM DECISION
    // ===============================
    ClaimDTO approveClaim(Long claimId, Double approvedAmount, String userEmail);

    ClaimDTO rejectClaim(Long claimId, String reason, String userEmail);

    // ===============================
    // COMPENSATION
    // ===============================
    CompensationDetailsDTO getCompensationDetails(Long claimId);

    // ===============================
    // 🔍 FRAUD DETECTION
    // ===============================
    boolean isFraudulent(Long claimId);

    // ===============================
    // 📊 STATISTICS
    // ===============================
    Map<String, Object> getStats();

    // ===============================
    // 🔍 SEARCH
    // ===============================
    List<ClaimDTO> search(String status, Double min, Double max);

    // ===============================
    // 💡 RECOMMENDATION
    // ===============================
    String getRecommendation(Long claimId);

    // ===============================
    // 📈 PREDICTION
    // ===============================
    Double predictClientCost(Long clientId);

    ClaimDTO decideClaimAutomatically(Long claimId);
}