package org.example.projet_pi.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.projet_pi.Dto.RiskEvaluationDTO;
import org.example.projet_pi.Service.RiskDisplayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/agent/risk")
@Tag(name = "🖥️ Interface HTML - Évaluation de Risque",
        description = "Pages HTML pour l'évaluation de risque")
public class RiskPageController {

    private final RiskDisplayService riskDisplayService;

    public RiskPageController(RiskDisplayService riskDisplayService) {
        this.riskDisplayService = riskDisplayService;
    }

    // Affichage avec path variable : /agent/risk/evaluation/1
    @GetMapping("/evaluation/{contractId}")
    @Operation(summary = "Page HTML d'évaluation de risque",
            description = "Affiche la page HTML d'évaluation d'un contrat via son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page HTML affichée avec succès"),
            @ApiResponse(responseCode = "404", description = "Contrat non trouvé")
    })
    public String showRiskEvaluationPage(
            @Parameter(description = "ID du contrat à évaluer", example = "1", required = true)
            @PathVariable Long contractId,
            Model model) {

        try {
            RiskEvaluationDTO evaluation = riskDisplayService.prepareRiskEvaluation(contractId);
            model.addAttribute("evaluationData", evaluation);
            model.addAttribute("hasData", true);
        } catch (Exception e) {
            model.addAttribute("hasData", false);
            model.addAttribute("error", "Contrat non trouvé : " + e.getMessage());
        }

        return "risk/evaluation"; // Nom du template Thymeleaf
    }
}