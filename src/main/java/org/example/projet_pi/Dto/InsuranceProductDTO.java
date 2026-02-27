package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceProductDTO {

    private Long productId;
    private String name;
    private String description;
    private Double basePrice;
    private String productType; // AUTO, HABITATION, SANTE

    private String status;
}