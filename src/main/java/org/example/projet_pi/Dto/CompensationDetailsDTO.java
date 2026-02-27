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
    private Double insurancePayment;
    private ClaimStatus status;

    public CompensationDetailsDTO(Long claimId, Double claimedAmount,
                                  Double approvedAmount, Double franchise,
                                  Double insurancePayment, ClaimStatus status) {
        this.claimId = claimId;
        this.claimedAmount = claimedAmount;
        this.approvedAmount = approvedAmount;
        this.franchise = franchise;
        this.insurancePayment = insurancePayment;
        this.status = status;
    }
}