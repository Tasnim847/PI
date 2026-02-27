package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompensationDTO {

    private Long compensationId;
    private Double amount;
    private Date paymentDate;

    private Long claimId; // on référence uniquement l'id du Claim
}