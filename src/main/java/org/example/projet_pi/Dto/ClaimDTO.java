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
public class ClaimDTO {

    private Long claimId;
    private Date claimDate;
    private Double claimedAmount;
    private Double approvedAmount;
    private String description;
    private String status;
    private String message;

    private Long clientId;
    private Long contractId;
    private Long compensationId;
    private List<Long> documentIds;
    private List<DocumentDTO> documents;

    private AutoClaimDetailsDTO autoDetails;
    private HealthClaimDetailsDTO healthDetails;
    private HomeClaimDetailsDTO homeDetails;
}
