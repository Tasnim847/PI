package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.projet_pi.entity.ProductType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceProductDTO {

    private Long productId;
    private String name;
    private String description;
    private Double basePrice;
    private ProductType productType; // AUTO, HABITATION, SANTE
    private String otherProductType;
    private String imageUrl;

    private String status;
}