package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeClaimDetailsDTO {

    private Long id;

    private String damageType;
    private String address;
    private Double estimatedLoss;
}