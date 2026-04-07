package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;
import org.example.projet_pi.entity.ClaimStatus;

@Getter
@Setter
public class CompensationDetailsDTO {
    private Long claimId;
    private Double claimedAmount;
    private Double approvedAmount;
    private Double franchise;
    private Double coverageLimit; // NOUVEAU
    private Double insurancePayment;
    private Double clientOutOfPocket; // NOUVEAU
    private ClaimStatus status;
    private String message; // NOUVEAU
    private boolean exceedsCoverageLimit; // NOUVEAU

    // Constructeur principal avec tous les champs
    public CompensationDetailsDTO(Long claimId, Double claimedAmount,
                                  Double approvedAmount, Double franchise,
                                  Double coverageLimit, Double insurancePayment,
                                  Double clientOutOfPocket, ClaimStatus status,
                                  String message) {
        this.claimId = claimId;
        this.claimedAmount = claimedAmount;
        this.approvedAmount = approvedAmount;
        this.franchise = franchise;
        this.coverageLimit = coverageLimit;
        this.insurancePayment = insurancePayment;
        this.clientOutOfPocket = clientOutOfPocket;
        this.status = status;
        this.message = message;
        this.exceedsCoverageLimit = approvedAmount > coverageLimit;
    }

    // Constructeur simplifié pour rétrocompatibilité
    public CompensationDetailsDTO(Long claimId, Double claimedAmount,
                                  Double approvedAmount, Double franchise,
                                  Double insurancePayment, ClaimStatus status) {
        this(claimId, claimedAmount, approvedAmount, franchise,
                0.0, insurancePayment, approvedAmount - insurancePayment,
                status, null);
    }
}