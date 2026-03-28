package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.ClaimScoreDTO;
import org.example.projet_pi.Dto.ClientScoreResult;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedClaimScoringService {

    private final ClaimRepository claimRepository;
    private final ClientScoringService clientScoringService;

    // Poids pour le scoring du claim
    private static final double WEIGHT_AMOUNT = 0.25;
    private static final double WEIGHT_DELAY = 0.20;
    private static final double WEIGHT_DOCUMENTS = 0.20;
    private static final double WEIGHT_FREQUENCY = 0.20;
    private static final double WEIGHT_CLIENT_HISTORY = 0.15;

    // Seuils de décision
    private static final double AUTO_APPROVE_THRESHOLD = 70.0;
    private static final double AUTO_REJECT_THRESHOLD = 40.0;

    /**
     * Calcule le score avancé d'un claim avec recommandation de décision
     */
    @Transactional(readOnly = true)
    public ClaimScoreDTO calculateAdvancedClaimScore(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

        Client client = claim.getClient();

        // 1. Score client global
        ClientScoreResult clientScore = clientScoringService.calculateClientScore(client.getId());
        double clientGlobalScore = clientScore.getGlobalScore();

        // 2. Analyse des risques spécifiques au claim
        Map<String, Double> riskFactors = analyzeRiskFactors(claim);

        // 🔥 LOGS DES FACTEURS DE RISQUE
        log.info("========== FACTEURS DE RISQUE ==========");
        log.info("Facteur montant: {}/100", riskFactors.getOrDefault("amount", 0.0));
        log.info("Facteur délai: {}/100", riskFactors.getOrDefault("delay", 0.0));
        log.info("Facteur documents: {}/100", riskFactors.getOrDefault("documents", 0.0));
        log.info("Facteur fréquence: {}/100", riskFactors.getOrDefault("frequency", 0.0));
        log.info("Facteur historique: {}/100", riskFactors.getOrDefault("history", 0.0));
        log.info("=========================================");

        // 3. Calcul du score du claim (0-100)
        double claimRiskScore = calculateClaimRiskScore(riskFactors);

        // 4. Score final combiné (client + claim)
        double finalScore = (clientGlobalScore * 0.6) + (claimRiskScore * 0.4);

        // 🔥 LOGS DU SCORE FINAL
        log.info("========== DÉTAIL SCORING ==========");
        log.info("Claim ID: {}", claimId);
        log.info("Montant réclamé: {} DT", claim.getClaimedAmount());
        log.info("Documents: {}", claim.getDocuments() != null ? claim.getDocuments().size() : 0);
        log.info("Score client: {}/100", clientGlobalScore);
        log.info("Score claim: {}/100", claimRiskScore);
        log.info("Score final: {}/100", finalScore);
        log.info("Seuil approbation: {} (>= {})", finalScore >= AUTO_APPROVE_THRESHOLD ? "✅ ATTEINT" : "❌ NON ATTEINT", AUTO_APPROVE_THRESHOLD);
        log.info("Seuil rejet: {} (< {})", finalScore < AUTO_REJECT_THRESHOLD ? "✅ ATTEINT" : "❌ NON ATTEINT", AUTO_REJECT_THRESHOLD);
        log.info("====================================");

        // 5. Déterminer la décision suggérée
        ClaimScoreDTO.DecisionSuggestion decision = determineDecision(finalScore, riskFactors);

        // 6. Générer la recommandation détaillée
        String recommendation = generateDetailedRecommendation(finalScore, riskFactors, decision);

        // 7. Déterminer le niveau de risque
        String riskLevel = determineRiskLevel(finalScore);
        String colorCode = getColorCode(riskLevel);

        return ClaimScoreDTO.builder()
                .claimId(claimId)
                .claimedAmount(claim.getClaimedAmount())
                .riskScore((int) finalScore)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .colorCode(colorCode)
                .delayInfo(riskFactors.getOrDefault("delay", 0.0).toString())
                .documentTypeInfo(getDocumentInfo(claim))
                .frequencyInfo(getFrequencyInfo(claim))
                .isSuspicious(finalScore < 50)
                .decisionSuggestion(decision)
                .build();
    }

    /**
     * Analyse les facteurs de risque du claim
     */
    private Map<String, Double> analyzeRiskFactors(Claim claim) {
        Map<String, Double> factors = new HashMap<>();

        // 1. Facteur montant (0-100, plus élevé = plus risqué)
        double amountFactor = calculateAmountRisk(claim);
        factors.put("amount", amountFactor);

        // 2. Facteur délai (0-100)
        double delayFactor = calculateDelayRisk(claim);
        factors.put("delay", delayFactor);

        // 3. Facteur documents (0-100)
        double documentFactor = calculateDocumentRisk(claim);
        factors.put("documents", documentFactor);

        // 4. Facteur fréquence (0-100)
        double frequencyFactor = calculateFrequencyRisk(claim);
        factors.put("frequency", frequencyFactor);

        // 5. Facteur historique client (0-100)
        double historyFactor = calculateClientHistoryRisk(claim.getClient());
        factors.put("history", historyFactor);

        return factors;
    }

    /**
     * Calcule le risque basé sur le montant
     */
    private double calculateAmountRisk(Claim claim) {
        double amount = claim.getClaimedAmount();
        Double coverageLimit = claim.getContract().getCoverageLimit();

        // Vérifier si dépasse le plafond
        if (amount > coverageLimit) {
            double percentageOver = (amount / coverageLimit) * 100;
            if (percentageOver > 200) return 100.0;
            if (percentageOver > 150) return 95.0;
            if (percentageOver > 100) return 90.0;
            return 85.0;
        }

        // Pourcentage du plafond utilisé
        double percentageOfLimit = (amount / coverageLimit) * 100;

        if (amount > 10000) return 80.0;
        if (amount > 5000) return 60.0;
        if (amount > 2000) return 40.0;
        if (amount > 1000) return 20.0;
        if (percentageOfLimit > 90) return 70.0;
        if (percentageOfLimit > 70) return 50.0;
        if (percentageOfLimit > 50) return 30.0;
        if (percentageOfLimit > 30) return 20.0;

        return 10.0;
    }

    /**
     * Calcule le risque basé sur le délai entre le début du contrat et le claim
     * ✅ CORRIGÉ: Utilisation de getTime() au lieu de toInstant()
     */
    private double calculateDelayRisk(Claim claim) {
        if (claim.getContract() == null || claim.getContract().getStartDate() == null || claim.getClaimDate() == null) {
            log.debug("Données manquantes pour calcul du délai");
            return 50.0;
        }

        try {
            // ✅ Utiliser getTime() pour éviter UnsupportedOperationException
            long startTime = claim.getContract().getStartDate().getTime();
            long claimTime = claim.getClaimDate().getTime();

            // Calculer la différence en jours
            long days = (claimTime - startTime) / (1000 * 60 * 60 * 24);

            log.debug("Délai calculé: {} jours entre contrat et claim", days);

            // Délai très court = suspect
            if (days < 30) return 85.0;
            if (days < 90) return 60.0;
            if (days < 180) return 40.0;
            if (days < 365) return 20.0;

            return 10.0;

        } catch (Exception e) {
            log.error("Erreur lors du calcul du délai: {}", e.getMessage());
            return 50.0;
        }
    }

    /**
     * Calcule le risque basé sur les documents fournis
     */
    private double calculateDocumentRisk(Claim claim) {
        int docCount = claim.getDocuments() != null ? claim.getDocuments().size() : 0;

        if (docCount == 0) return 100.0;
        if (docCount == 1) return 60.0;
        if (docCount == 2) return 30.0;

        // Vérifier les types de documents
        boolean hasRequiredDocs = claim.getDocuments().stream()
                .anyMatch(doc -> doc.getType() != null &&
                        (doc.getType().toUpperCase().contains("FACTURE") ||
                                doc.getType().toUpperCase().contains("DEVIS") ||
                                doc.getType().toUpperCase().contains("RAPPORT")));

        if (!hasRequiredDocs) return 70.0;

        return 10.0;
    }

    /**
     * Calcule le risque basé sur la fréquence des claims
     * ✅ CORRIGÉ: Utilisation de getTime() pour la comparaison
     */
    private double calculateFrequencyRisk(Claim claim) {
        List<Claim> clientClaims = claimRepository.findByClientId(claim.getClient().getId());

        if (clientClaims.isEmpty()) {
            return 10.0;
        }

        try {
            // ✅ Utiliser System.currentTimeMillis() pour éviter les problèmes de conversion
            long oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000);

            long recentClaims = clientClaims.stream()
                    .filter(c -> c.getClaimDate() != null && c.getClaimDate().getTime() > oneYearAgo)
                    .count();

            log.debug("Claims récents (12 mois): {}", recentClaims);

            if (recentClaims > 5) return 90.0;
            if (recentClaims > 3) return 70.0;
            if (recentClaims > 2) return 50.0;
            if (recentClaims > 1) return 30.0;

            return 10.0;

        } catch (Exception e) {
            log.error("Erreur lors du calcul de la fréquence: {}", e.getMessage());
            return 50.0;
        }
    }

    /**
     * Calcule le risque basé sur l'historique du client
     */
    private double calculateClientHistoryRisk(Client client) {
        List<Claim> claims = claimRepository.findByClientId(client.getId());

        long rejectedClaims = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.REJECTED)
                .count();

        long approvedClaims = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                .count();

        double riskScore = 0.0;

        if (rejectedClaims > 0) {
            riskScore += Math.min(40, rejectedClaims * 15);
        }

        if (approvedClaims > 0 && rejectedClaims == 0) {
            riskScore -= Math.min(20, approvedClaims * 5);
        }

        return Math.max(0, Math.min(100, 50 + riskScore));
    }

    /**
     * Calcule le score de risque du claim (0-100)
     */
    private double calculateClaimRiskScore(Map<String, Double> riskFactors) {
        double amountRisk = riskFactors.getOrDefault("amount", 50.0);
        double delayRisk = riskFactors.getOrDefault("delay", 50.0);
        double documentRisk = riskFactors.getOrDefault("documents", 50.0);
        double frequencyRisk = riskFactors.getOrDefault("frequency", 50.0);
        double historyRisk = riskFactors.getOrDefault("history", 50.0);

        double riskScore = (amountRisk * WEIGHT_AMOUNT) +
                (delayRisk * WEIGHT_DELAY) +
                (documentRisk * WEIGHT_DOCUMENTS) +
                (frequencyRisk * WEIGHT_FREQUENCY) +
                (historyRisk * WEIGHT_CLIENT_HISTORY);

        return Math.max(0, Math.min(100, riskScore));
    }

    /**
     * Détermine la décision suggérée basée sur le score final
     */
    private ClaimScoreDTO.DecisionSuggestion determineDecision(double finalScore, Map<String, Double> riskFactors) {
        if (finalScore >= AUTO_APPROVE_THRESHOLD) {
            return ClaimScoreDTO.DecisionSuggestion.AUTO_APPROVE;
        }

        if (finalScore < AUTO_REJECT_THRESHOLD) {
            return ClaimScoreDTO.DecisionSuggestion.AUTO_REJECT;
        }

        if (riskFactors.getOrDefault("amount", 0.0) > 80 ||
                riskFactors.getOrDefault("delay", 0.0) > 80 ||
                riskFactors.getOrDefault("documents", 0.0) > 80) {
            return ClaimScoreDTO.DecisionSuggestion.MANUAL_REVIEW;
        }

        return ClaimScoreDTO.DecisionSuggestion.MANUAL_REVIEW;
    }

    /**
     * Génère une recommandation détaillée
     */
    private String generateDetailedRecommendation(double finalScore,
                                                  Map<String, Double> riskFactors,
                                                  ClaimScoreDTO.DecisionSuggestion decision) {
        StringBuilder rec = new StringBuilder();

        rec.append(String.format("Score global: %.1f/100\n", finalScore));
        rec.append("Facteurs de risque analysés:\n");

        rec.append(String.format("- Montant: %.0f/100\n", riskFactors.getOrDefault("amount", 0.0)));
        rec.append(String.format("- Délai: %.0f/100\n", riskFactors.getOrDefault("delay", 0.0)));
        rec.append(String.format("- Documents: %.0f/100\n", riskFactors.getOrDefault("documents", 0.0)));
        rec.append(String.format("- Fréquence: %.0f/100\n", riskFactors.getOrDefault("frequency", 0.0)));
        rec.append(String.format("- Historique: %.0f/100\n", riskFactors.getOrDefault("history", 0.0)));

        rec.append("\nDécision suggérée: ");
        switch (decision) {
            case AUTO_APPROVE:
                rec.append("✅ APPROUVER AUTOMATIQUEMENT - Risque faible\n");
                break;
            case AUTO_REJECT:
                rec.append("❌ REJETER AUTOMATIQUEMENT - Risque très élevé\n");
                break;
            case MANUAL_REVIEW:
                rec.append("⚠️ REVUE MANUELLE REQUISE - Analyse approfondie nécessaire\n");
                break;
        }

        if (riskFactors.getOrDefault("amount", 0.0) > 70) {
            rec.append("\n⚠️ Montant anormalement élevé - Vérifier la justification\n");
        }
        if (riskFactors.getOrDefault("delay", 0.0) > 70) {
            rec.append("\n⚠️ Claim déclaré très tôt après souscription - Vérifier les circonstances\n");
        }
        if (riskFactors.getOrDefault("documents", 0.0) > 70) {
            rec.append("\n⚠️ Documentation insuffisante - Demander des justificatifs supplémentaires\n");
        }
        if (riskFactors.getOrDefault("frequency", 0.0) > 70) {
            rec.append("\n⚠️ Fréquence anormale de claims - Surveillance renforcée\n");
        }

        return rec.toString();
    }

    private String determineRiskLevel(double score) {
        if (score >= 80) return "TRES_FAIBLE";
        if (score >= 65) return "FAIBLE";
        if (score >= 50) return "MODERE";
        if (score >= 35) return "ELEVE";
        return "TRES_ELEVE";
    }

    private String getColorCode(String riskLevel) {
        switch (riskLevel) {
            case "TRES_FAIBLE": return "🟢";
            case "FAIBLE": return "🟢";
            case "MODERE": return "🟡";
            case "ELEVE": return "🟠";
            case "TRES_ELEVE": return "🔴";
            default: return "⚪";
        }
    }

    private String getDocumentInfo(Claim claim) {
        int count = claim.getDocuments() != null ? claim.getDocuments().size() : 0;
        return String.format("%d document(s)", count);
    }

    private String getFrequencyInfo(Claim claim) {
        List<Claim> claims = claimRepository.findByClientId(claim.getClient().getId());
        return String.format("%d claim(s) au total", claims.size());
    }
}