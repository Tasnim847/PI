package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CompensationDTO {

    private Long compensationId;
    private double amount;
    private Date paymentDate;

    private Long claimId; // on référence uniquement l'id du Claim
}