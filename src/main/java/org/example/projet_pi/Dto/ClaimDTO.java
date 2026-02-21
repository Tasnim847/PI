package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ClaimDTO {

    private Long claimId;
    private Date claimDate;
    private double claimedAmount;
    private double approvedAmount;
    private String description;
    private String status;

    private Long clientId;
    private Long contractId;
    private Long compensationId;
    private Long riskClaimId;
    private List<Long> documentIds;
    private List<DocumentDTO> documents;
}
