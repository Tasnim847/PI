package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.ClaimScoreDTO;
import org.example.projet_pi.Dto.ClientScoreResult;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Service.AdvancedClaimScoringService;
import org.example.projet_pi.Service.ClaimService;
import org.example.projet_pi.Service.ClientScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scoring")
public class ScoringController {

    private final ClientScoringService scoringService;
    private final ClaimService claimService;
    private final AdvancedClaimScoringService advancedClaimScoringService;
    private final ClaimRepository claimRepository;

    public ScoringController(ClientScoringService scoringService , ClaimService claimService, AdvancedClaimScoringService advancedClaimScoringService, ClaimRepository claimRepository) {
        this.scoringService = scoringService;
        this.claimService = claimService;
        this.advancedClaimScoringService = advancedClaimScoringService;
        this.claimRepository = claimRepository;
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ClientScoreResult> getClientScore(@PathVariable Long clientId) {
        ClientScoreResult result = scoringService.calculateClientScore(clientId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/client/{clientId}/summary")
    public ResponseEntity<?> getClientScoreSummary(@PathVariable Long clientId) {
        ClientScoreResult result = scoringService.calculateClientScore(clientId);

        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "score", result.getGlobalScore(),
                "riskLevel", result.getRiskLevel(),
                "riskClass", result.getRiskClass(),
                "recommendations", result.getRecommendations()
        ));
    }

    @GetMapping("/client/{clientId}/claims-advanced")
    public ResponseEntity<?> getClientClaimsAdvanced(@PathVariable Long clientId) {

        // Score global du client
        ClientScoreResult clientScore = scoringService.calculateClientScore(clientId);

        // Score détaillé des claims
        List<ClaimScoreDTO> claimsScore = scoringService.getClaimsScoreAdvanced(clientId);

        // Retour JSON combiné
        return ResponseEntity.ok(Map.of(
                "clientScore", clientScore,
                "claimsScore", claimsScore
        ));
    }


    // Dans ScoringController, ajoutez ces endpoints

    @GetMapping("/claim/{claimId}/advanced")
    public ResponseEntity<ClaimScoreDTO> getAdvancedClaimScore(@PathVariable Long claimId) {
        ClaimScoreDTO score = advancedClaimScoringService.calculateAdvancedClaimScore(claimId);
        return ResponseEntity.ok(score);
    }

    @PostMapping("/claim/{claimId}/auto-decision-advanced")
    public ResponseEntity<ClaimDTO> autoDecisionWithAdvancedScoring(@PathVariable Long claimId) {
        ClaimDTO result = claimService.decideClaimAutomaticallyWithAdvancedScoring(claimId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/claim/{claimId}/detailed-analysis")
    public ResponseEntity<Map<String, Object>> getDetailedClaimAnalysis(@PathVariable Long claimId) {
        ClaimScoreDTO score = advancedClaimScoringService.calculateAdvancedClaimScore(claimId);
        ClientScoreResult clientScore = scoringService.calculateClientScore(
                claimRepository.findById(claimId).get().getClient().getId()
        );

        return ResponseEntity.ok(Map.of(
                "claimScore", score,
                "clientScore", clientScore,
                "finalScore", Map.of(
                        "value", score.getRiskScore(),
                        "level", score.getRiskLevel(),
                        "decision", score.getDecisionSuggestion(),
                        "isSuspicious", score.isSuspicious()
                )
        ));
    }
}