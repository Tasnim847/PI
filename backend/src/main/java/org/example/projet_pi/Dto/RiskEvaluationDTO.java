package org.example.projet_pi.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RiskEvaluationDTO {
    // Informations générales
    private Long contractId;
    private String contractReference;
    private String clientName;
    private String agentName;
    private Date evaluationDate;

    // Score global
    private Double globalRiskScore;
    private String globalRiskLevel;
    private String globalRiskClass;
    private String recommendation;
    private Boolean autoReject;  // Boolean objet (peut être null)

    // Détails par catégorie
    private Map<String, CategoryRiskDTO> categories;

    // Facteurs de risque
    private List<RiskFactorDTO> riskFactors;

    // Points positifs
    private List<String> positivePoints;

    // Actions recommandées
    private List<String> recommendedActions;

    // Rapport détaillé
    private String detailedReport;

    /**
     * Méthode helper pour éviter les problèmes de null
     * Utilisez cette méthode au lieu d'accéder directement à autoReject
     */
    public boolean isAutoReject() {
        return autoReject != null && autoReject;
    }

    /**
     * Méthode pour obtenir la valeur par défaut si null
     */
    public boolean getAutoRejectOrDefault(boolean defaultValue) {
        return autoReject != null ? autoReject : defaultValue;
    }
}