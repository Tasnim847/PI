package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceContractDTO {

    private Long contractId;
    private Date startDate;
    private Date endDate;
    private Double premium;
    private Double deductible;
    private Double coverageLimit;
    private String status;
    private String paymentFrequency;

    private Long clientId;
    private Long productId;
    private Long agentAssuranceId;

    private List<Long> claimIds;
    private List<Long> paymentIds;

    // 🔥 NOUVEAUX CHAMPS
    private Double totalPaid;
    private Double remainingAmount;
}