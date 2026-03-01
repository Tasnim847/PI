package org.example.projet_pi.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.CategoryRiskDTO;
import org.example.projet_pi.Dto.DecisionRequest;
import org.example.projet_pi.Dto.RiskEvaluationDTO;
import org.example.projet_pi.Dto.RiskFactorDTO;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Service.RiskDisplayService;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/risk")
public class RiskDisplayController {

    private final RiskDisplayService riskDisplayService;
    private final InsuranceContractRepository contractRepository;

    public RiskDisplayController(
            RiskDisplayService riskDisplayService,
            InsuranceContractRepository contractRepository) {
        this.riskDisplayService = riskDisplayService;
        this.contractRepository = contractRepository;
    }

    /**
     * Endpoint pour l'agent : visualiser le calcul de risque avant décision
     */
    @GetMapping("/evaluation/{contractId}")
    public ResponseEntity<?> getRiskEvaluationForAgent(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            log.info("📊 Agent {} consulte l'évaluation de risque pour contrat {}",
                    currentUser.getUsername(), contractId);

            RiskEvaluationDTO evaluation = riskDisplayService.prepareRiskEvaluation(contractId);

            // Statistiques pour l'affichage
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("evaluation", evaluation);
            response.put("decision_buttons", generateDecisionButtons(evaluation));
            response.put("summary", generateSummary(evaluation));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de l'évaluation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Endpoint pour prendre une décision basée sur l'évaluation
     */
    @PostMapping("/decide/{contractId}")
    public ResponseEntity<?> makeDecision(
            @PathVariable Long contractId,
            @RequestBody DecisionRequest decisionRequest,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            log.info("📋 Agent {} prend une décision pour contrat {}: {}",
                    currentUser.getUsername(), contractId, decisionRequest.getDecision());

            // Ici vous pouvez appeler votre service pour appliquer la décision
            // contractService.applyDecision(contractId, decisionRequest, currentUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("contractId", contractId);
            response.put("decision", decisionRequest.getDecision());
            response.put("comment", decisionRequest.getComment());
            response.put("timestamp", new Date());
            response.put("message", generateDecisionMessage(decisionRequest.getDecision()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la prise de décision: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Résumé exécutif pour affichage rapide
     */
    @GetMapping("/summary/{contractId}")
    public ResponseEntity<?> getRiskSummary(@PathVariable Long contractId) {
        try {
            RiskEvaluationDTO evaluation = riskDisplayService.prepareRiskEvaluation(contractId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("contractId", contractId);
            summary.put("riskScore", evaluation.getGlobalRiskScore());
            summary.put("riskLevel", evaluation.getGlobalRiskLevel());
            summary.put("riskClass", evaluation.getGlobalRiskClass());
            summary.put("recommendation", evaluation.getRecommendation());
            summary.put("autoReject", evaluation.isAutoReject()); // Utilisation de la méthode helper

            // Gestion des listes null
            List<RiskFactorDTO> riskFactors = evaluation.getRiskFactors();
            summary.put("keyFactors", riskFactors != null ?
                    riskFactors.stream().limit(3).map(RiskFactorDTO::getFactor).toList() :
                    List.of());

            List<String> positivePoints = evaluation.getPositivePoints();
            summary.put("positivePoints", positivePoints != null ?
                    positivePoints.stream().limit(2).toList() :
                    List.of());

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    private Map<String, Object> generateDecisionButtons(RiskEvaluationDTO evaluation) {
        Map<String, Object> buttons = new HashMap<>();

        // CORRECTION: Utilisation de isAutoReject() au lieu de autoReject directement
        if (evaluation.isAutoReject()) {
            buttons.put("primary", List.of(
                    Map.of("action", "reject", "label", "❌ Confirmer le rejet", "style", "danger")
            ));
            buttons.put("secondary", List.of(
                    Map.of("action", "review", "label", "🔄 Forcer la révision", "style", "warning")
            ));
        } else if ("MEDIUM".equals(evaluation.getGlobalRiskLevel())) {
            buttons.put("primary", List.of(
                    Map.of("action", "accept", "label", "✅ Accepter avec conditions", "style", "success"),
                    Map.of("action", "reject", "label", "❌ Rejeter", "style", "danger")
            ));
            buttons.put("secondary", List.of(
                    Map.of("action", "request_info", "label", "📋 Demander des infos", "style", "info")
            ));
        } else {
            buttons.put("primary", List.of(
                    Map.of("action", "accept", "label", "✅ Accepter", "style", "success"),
                    Map.of("action", "reject", "label", "❌ Rejeter", "style", "danger")
            ));
        }

        return buttons;
    }

    private Map<String, Object> generateSummary(RiskEvaluationDTO evaluation) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("title", "Résumé de l'évaluation");
        summary.put("score", String.format("%.1f/100", evaluation.getGlobalRiskScore()));
        summary.put("level", evaluation.getGlobalRiskLevel());
        summary.put("class", evaluation.getGlobalRiskClass());

        // Gestion des catégories null
        Map<String, CategoryRiskDTO> categories = evaluation.getCategories();
        if (categories != null) {
            summary.put("categories", categories.values().stream()
                    .map(c -> Map.of(
                            "name", c.getCategoryName(),
                            "score", c.getScore(),
                            "level", c.getRiskLevel()
                    ))
                    .toList());
        } else {
            summary.put("categories", List.of());
        }

        return summary;
    }

    private String generateDecisionMessage(String decision) {
        if (decision == null) return "Décision enregistrée";

        switch (decision.toLowerCase()) {
            case "accept":
                return "✅ Contrat accepté - Une confirmation sera envoyée au client";
            case "reject":
                return "❌ Contrat rejeté - Le client sera notifié par email";
            case "request_info":
                return "📋 Demande d'informations complémentaires envoyée au client";
            case "review":
                return "🔄 Contrat marqué pour révision manuelle";
            default:
                return "Décision enregistrée";
        }
    }
}