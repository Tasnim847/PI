package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.CreditScoreDTO;
import org.example.projet_pi.Service.CreditScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/Scoring")
public class CreditScoringController {

    @Autowired
    private CreditScoringService creditScoringService;

    // ===============================
    // CALCULER LE SCORE D'UN CLIENT - ADMIN ET AGENT_FINANCE SEULEMENT
    // ===============================
    @GetMapping("/calculate/{clientId}")
    public ResponseEntity<?> calculateCreditScore(
            @PathVariable Long clientId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN") && !hasRole(currentUser, "AGENT_FINANCE")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", 
                                "message", "Réservé aux admins et agents finance"));
            }

            CreditScoreDTO scoreResult = creditScoringService.calculateCreditScore(clientId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Score calculé avec succès",
                "data", scoreResult
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Client non trouvé", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du calcul du score", "message", e.getMessage()));
        }
    }

    // ===============================
    // ANALYSER UN PROFIL AVEC GEMINI - ADMIN ET AGENT_FINANCE SEULEMENT
    // ===============================
    @PostMapping("/analyze/{clientId}")
    public ResponseEntity<?> analyzeClientProfile(
            @PathVariable Long clientId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN") && !hasRole(currentUser, "AGENT_FINANCE")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", 
                                "message", "Réservé aux admins et agents finance"));
            }

            CreditScoreDTO scoreResult = creditScoringService.calculateCreditScore(clientId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Analyse complétée avec succès",
                "clientId", clientId,
                "score", scoreResult.getScore(),
                "riskLevel", scoreResult.getRiskLevel(),
                "recommendation", scoreResult.getRecommendation(),
                "analysis", scoreResult.getAnalysis(),
                "calculatedAt", scoreResult.getCalculatedAt(),
                "metrics", Map.of(
                    "totalCredits", scoreResult.getTotalCredits(),
                    "activeCredits", scoreResult.getActiveCredits(),
                    "closedCredits", scoreResult.getClosedCredits(),
                    "totalAmount", scoreResult.getTotalAmount(),
                    "currentDebt", scoreResult.getCurrentDebt(),
                    "averageLatePercentage", scoreResult.getAverageLatePercentage(),
                    "averageMonthlyPayment", scoreResult.getAverageMonthlyPayment(),
                    "daysSinceLastCredit", scoreResult.getDaysSinceLastCredit()
                )
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Client non trouvé", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'analyse", "message", e.getMessage()));
        }
    }

    // ===============================
    // OBTENIR LE SCORE RAPIDE - ADMIN ET AGENT_FINANCE SEULEMENT
    // ===============================
    @GetMapping("/quick-score/{clientId}")
    public ResponseEntity<?> getQuickScore(
            @PathVariable Long clientId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            System.out.println("=== QUICK SCORE REQUEST ===");
            System.out.println("Client ID: " + clientId);
            System.out.println("User: " + (currentUser != null ? currentUser.getUsername() : "null"));
            
            if (currentUser == null) {
                System.out.println("ERROR: currentUser is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Non authentifié"));
            }
            
            if (!hasRole(currentUser, "ADMIN") && !hasRole(currentUser, "AGENT_FINANCE")) {
                System.out.println("ERROR: User doesn't have required role");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé"));
            }

            System.out.println("Calling creditScoringService.calculateCreditScore...");
            CreditScoreDTO scoreResult = creditScoringService.calculateCreditScore(clientId);
            System.out.println("Score calculated successfully: " + scoreResult.getScore());
            
            // Vérifier que les valeurs ne sont pas null avant de créer la réponse
            Integer score = scoreResult.getScore() != null ? scoreResult.getScore() : 0;
            String riskLevel = scoreResult.getRiskLevel() != null ? scoreResult.getRiskLevel() : "INCONNU";
            String recommendation = scoreResult.getRecommendation() != null ? scoreResult.getRecommendation() : "EN_ATTENTE";
            
            return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "score", score,
                "riskLevel", riskLevel,
                "recommendation", recommendation
            ));

        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: IllegalArgumentException - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Client non trouvé", "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("ERROR: Exception - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du calcul", "message", e.getMessage()));
        }
    }

    // ===============================
    // UTILITAIRES
    // ===============================
    private boolean hasRole(UserDetails userDetails, String role) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}