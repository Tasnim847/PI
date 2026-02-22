package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceProductDTO {

    private Long productId;
    private String name;
    private String description;
    private double basePrice;
    private String productType; // AUTO, HABITATION, SANTE

    private String status;
}