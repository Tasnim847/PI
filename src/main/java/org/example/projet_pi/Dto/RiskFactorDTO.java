package org.example.projet_pi.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskFactorDTO {
    private String factor;
    private String impact; // HIGH, MEDIUM, LOW
    private Double points;
    private String description;
}
