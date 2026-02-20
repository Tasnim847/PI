package org.example.projet_pi.Dto;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class InsuranceContractDTO {

    private Long contractId;
    private Date startDate;
    private Date endDate;
    private double premium;
    private double deductible;
    private double coverageLimit;
    private String status;

    private Long clientId;
    private Long productId;
    private Long agentAssuranceId;

    private List<Long> claimIds;
    private List<Long> paymentIds;
}
