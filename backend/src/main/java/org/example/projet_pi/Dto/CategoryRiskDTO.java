package org.example.projet_pi.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryRiskDTO {
    private String categoryName;
    private Double score;
    private Double weight;
    private String riskLevel;
    private String description;
    private List<String> details;
}