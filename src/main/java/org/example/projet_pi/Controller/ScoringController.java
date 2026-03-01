package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ClientScoreResult;
import org.example.projet_pi.Service.ClientScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scoring")
public class ScoringController {

    private final ClientScoringService scoringService;

    public ScoringController(ClientScoringService scoringService) {
        this.scoringService = scoringService;
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
}