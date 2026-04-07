package org.example.projet_pi.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data
@Builder
public class ClientScoreResult {
    private Long clientId;
    private Double globalScore;
    private String riskLevel;
    private String riskClass;
    private Map<String, Double> componentScores;
    private Map<String, String> recommendations;
    private Date calculationDate;
}