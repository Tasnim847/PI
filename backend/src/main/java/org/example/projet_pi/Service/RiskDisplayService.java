package org.example.projet_pi.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.*;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.Repository.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RiskDisplayService {

    private final InsuranceContractRepository contractRepository;
    private final ClientScoringService clientScoringService;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;

    public RiskDisplayService(
            InsuranceContractRepository contractRepository,
            ClientScoringService clientScoringService,
            ClaimRepository claimRepository,
            PaymentRepository paymentRepository) {
        this.contractRepository = contractRepository;
        this.clientScoringService = clientScoringService;
        this.claimRepository = claimRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Prépare l'affichage du calcul de risque pour un contrat
     */
    public RiskEvaluationDTO prepareRiskEvaluation(Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        Client client = contract.getClient();
        RiskClaim riskClaim = contract.getRiskClaim();

        if (riskClaim == null) {
            throw new RuntimeException("Aucune évaluation de risque trouvée pour ce contrat");
        }

        // Récupérer le score client
        ClientScoreResult clientScore = clientScoringService.calculateClientScore(client.getId());

        // Construire les catégories de risque
        Map<String, CategoryRiskDTO> categories = buildRiskCategories(contract, client, clientScore, riskClaim);

        // Extraire les facteurs de risque
        List<RiskFactorDTO> riskFactors = extractRiskFactors(riskClaim, categories);

        // Points positifs
        List<String> positivePoints = extractPositivePoints(categories);

        // Actions recommandées
        List<String> recommendedActions = generateRecommendedActions(riskClaim, clientScore, contract);

        return RiskEvaluationDTO.builder()
                .contractId(contract.getContractId())
                .contractReference("CTR-" + contract.getContractId())
                .clientName(client.getFirstName() + " " + client.getLastName())
                .agentName(client.getAgentAssurance() != null ?
                        client.getAgentAssurance().getFirstName() + " " + client.getAgentAssurance().getLastName() :
                        "Non assigné")
                .evaluationDate(new Date())
                .globalRiskScore(riskClaim.getRiskScore())
                .globalRiskLevel(riskClaim.getRiskLevel())
                .globalRiskClass(determineRiskClass(riskClaim.getRiskScore()))
                .recommendation(generateRecommendation(riskClaim))
                .autoReject("HIGH".equals(riskClaim.getRiskLevel()))
                .categories(categories)
                .riskFactors(riskFactors)
                .positivePoints(positivePoints)
                .recommendedActions(recommendedActions)
                .detailedReport(riskClaim.getEvaluationNote())
                .build();
    }

    /**
     * Construit les catégories de risque
     */
    private Map<String, CategoryRiskDTO> buildRiskCategories(
            InsuranceContract contract,
            Client client,
            ClientScoreResult clientScore,
            RiskClaim riskClaim) {

        Map<String, CategoryRiskDTO> categories = new LinkedHashMap<>();

        // 1. Catégorie CLIENT
        CategoryRiskDTO clientCategory = CategoryRiskDTO.builder()
                .categoryName("Profil Client")
                .score(clientScore.getGlobalScore())
                .weight(0.30)
                .riskLevel(clientScore.getRiskLevel())
                .description("Évaluation du profil démographique, financier et comportemental")
                .details(Arrays.asList(
                        String.format("Âge: %d ans", client.getAge()),
                        String.format("Revenus: %.2f DT", client.getAnnualIncome() != null ? client.getAnnualIncome() : 0),
                        String.format("Statut: %s", client.getEmploymentStatus() != null ? client.getEmploymentStatus() : "Non renseigné"),
                        String.format("Ancienneté: %d jours", client.getClientTenureInDays())
                ))
                .build();
        categories.put("client", clientCategory);

        // 2. Catégorie PRODUIT
        double productScore = calculateProductScore(contract);
        CategoryRiskDTO productCategory = CategoryRiskDTO.builder()
                .categoryName("Type de Produit")
                .score(productScore)
                .weight(0.20)
                .riskLevel(scoreToLevel(productScore))
                .description("Évaluation du risque lié au type de produit d'assurance")
                .details(Arrays.asList(
                        String.format("Produit: %s", contract.getProduct() != null ? contract.getProduct().getName() : "N/A"),
                        String.format("Type: %s", contract.getProduct() != null ? contract.getProduct().getProductType() : "N/A"),
                        String.format("Prix de base: %.2f DT", contract.getProduct() != null ? contract.getProduct().getBasePrice() : 0)
                ))
                .build();
        categories.put("product", productCategory);

        // 3. Catégorie FINANCIERE
        double financialScore = calculateFinancialScore(contract, client);
        CategoryRiskDTO financialCategory = CategoryRiskDTO.builder()
                .categoryName("Aspects Financiers")
                .score(financialScore)
                .weight(0.30)
                .riskLevel(scoreToLevel(financialScore))
                .description("Analyse de la prime, franchise et plafond")
                .details(Arrays.asList(
                        String.format("Prime: %.2f DT", contract.getPremium()),
                        String.format("Franchise: %.2f DT", contract.getDeductible()),
                        String.format("Plafond: %.2f DT", contract.getCoverageLimit()),
                        String.format("Ratio Prime/Revenus: %.2f%%",
                                client.getAnnualIncome() != null ?
                                        (contract.getPremium() / client.getAnnualIncome() * 100) : 0)
                ))
                .build();
        categories.put("financial", financialCategory);

        // 4. Catégorie TEMPORELLE
        double temporalScore = calculateTemporalScore(contract, client);
        CategoryRiskDTO temporalCategory = CategoryRiskDTO.builder()
                .categoryName("Durée et Temporalité")
                .score(temporalScore)
                .weight(0.20)
                .riskLevel(scoreToLevel(temporalScore))
                .description("Analyse de la durée du contrat et de l'âge du client")
                .details(Arrays.asList(
                        String.format("Durée: %d ans", getContractDuration(contract)),
                        String.format("Date début: %s", formatDate(contract.getStartDate())),
                        String.format("Date fin: %s", formatDate(contract.getEndDate())),
                        String.format("Âge fin contrat: %d ans",
                                client.getAge() + getContractDuration(contract))
                ))
                .build();
        categories.put("temporal", temporalCategory);

        return categories;
    }

    /**
     * Extrait les facteurs de risque
     */
    private List<RiskFactorDTO> extractRiskFactors(RiskClaim riskClaim, Map<String, CategoryRiskDTO> categories) {
        List<RiskFactorDTO> factors = new ArrayList<>();

        // Analyser le texte d'évaluation pour extraire les facteurs
        String evaluation = riskClaim.getEvaluationNote();

        if (evaluation.contains("Client à risque") ||
                (categories.containsKey("client") && categories.get("client").getScore() < 50)) {
            factors.add(RiskFactorDTO.builder()
                    .factor("Profil Client Défavorable")
                    .impact("HIGH")
                    .points(30.0)
                    .description("Le profil du client présente des risques significatifs")
                    .build());
        }

        if (categories.containsKey("financial") && categories.get("financial").getScore() > 60) {
            factors.add(RiskFactorDTO.builder()
                    .factor("Charge Financière Élevée")
                    .impact("MEDIUM")
                    .points(25.0)
                    .description("La prime est élevée par rapport aux revenus")
                    .build());
        }

        if (categories.containsKey("temporal") && categories.get("temporal").getScore() > 50) {
            factors.add(RiskFactorDTO.builder()
                    .factor("Durée Excessive")
                    .impact("MEDIUM")
                    .points(20.0)
                    .description("La durée du contrat est longue")
                    .build());
        }

        // Ajouter d'autres facteurs basés sur l'analyse
        if (evaluation.contains("franchise très basse")) {
            factors.add(RiskFactorDTO.builder()
                    .factor("Franchise Trop Basse")
                    .impact("HIGH")
                    .points(20.0)
                    .description("La franchise est trop basse, augmentant le risque de sinistres")
                    .build());
        }

        if (evaluation.contains("plafond très élevé")) {
            factors.add(RiskFactorDTO.builder()
                    .factor("Plafond Excessif")
                    .impact("HIGH")
                    .points(25.0)
                    .description("Le plafond de couverture est très élevé")
                    .build());
        }

        return factors;
    }

    /**
     * Extrait les points positifs
     */
    private List<String> extractPositivePoints(Map<String, CategoryRiskDTO> categories) {
        List<String> positives = new ArrayList<>();

        categories.forEach((key, category) -> {
            if (category.getScore() < 40) {
                positives.add(String.format("✅ %s: Risque faible (%.1f/100)",
                        category.getCategoryName(), category.getScore()));
            }
        });

        if (positives.isEmpty()) {
            positives.add("Aucun point particulièrement positif identifié");
        }

        return positives;
    }

    /**
     * Génère les actions recommandées
     */
    private List<String> generateRecommendedActions(RiskClaim riskClaim, ClientScoreResult clientScore, InsuranceContract contract) {
        List<String> actions = new ArrayList<>();

        String riskLevel = riskClaim.getRiskLevel();

        if ("HIGH".equals(riskLevel)) {
            actions.add("🔴 REJET AUTOMATIQUE - Contrat trop risqué");
            actions.add("   • Contacter le client pour lui expliquer les raisons");
            actions.add("   • Proposer des alternatives avec des garanties réduites");
        } else if ("MEDIUM".equals(riskLevel)) {
            actions.add("🟡 ANALYSE APPROFONDIE REQUISE");
            actions.add("   • Vérifier les points de vigilance identifiés");
            actions.add("   • Contacter le client pour clarifier certains points");
            actions.add("   • Envisager une augmentation de la franchise");
            if (contract.getPremium() > 5000) {
                actions.add("   • Proposer un paiement mensuel pour réduire le risque");
            }
        } else {
            actions.add("🟢 CONTRAT ACCEPTABLE");
            actions.add("   • Procéder à l'activation du contrat");
            actions.add("   • Envoyer la confirmation au client");
            actions.add("   • Planifier le suivi standard");
        }

        // Ajouter les recommandations du scoring client
        if (clientScore.getRecommendations() != null && !clientScore.getRecommendations().isEmpty()) {
            actions.add("\n📋 Recommandations spécifiques au client:");
            clientScore.getRecommendations().values().forEach(actions::add);
        }

        return actions;
    }

    /**
     * Calcule le score produit
     */
    private double calculateProductScore(InsuranceContract contract) {
        if (contract.getProduct() == null) return 50.0;

        String productType = contract.getProduct().getProductType();
        if ("VIE".equalsIgnoreCase(productType)) return 80.0;
        if ("SANTE".equalsIgnoreCase(productType)) return 60.0;
        if ("AUTO".equalsIgnoreCase(productType)) return 40.0;
        if ("HABITATION".equalsIgnoreCase(productType)) return 30.0;
        return 50.0;
    }

    /**
     * Calcule le score financier
     */
    private double calculateFinancialScore(InsuranceContract contract, Client client) {
        double score = 0;

        // Prime par rapport aux revenus
        if (client.getAnnualIncome() != null && client.getAnnualIncome() > 0) {
            double ratio = (contract.getPremium() / client.getAnnualIncome()) * 100;
            if (ratio > 30) score += 80;
            else if (ratio > 20) score += 60;
            else if (ratio > 10) score += 40;
            else score += 20;
        }

        // Franchise
        if (contract.getDeductible() < 200) score += 30;
        else if (contract.getDeductible() < 500) score += 20;
        else score += 10;

        // Plafond
        if (contract.getCoverageLimit() > 200000) score += 40;
        else if (contract.getCoverageLimit() > 100000) score += 30;
        else if (contract.getCoverageLimit() > 50000) score += 20;
        else score += 10;

        return Math.min(100, score / 3);
    }

    /**
     * Calcule le score temporel
     */
    private double calculateTemporalScore(InsuranceContract contract, Client client) {
        double score = 0;

        int duration = getContractDuration(contract);
        if (duration > 10) score += 80;
        else if (duration > 5) score += 60;
        else if (duration > 3) score += 40;
        else score += 20;

        int ageAtEnd = client.getAge() + duration;
        if (ageAtEnd > 75) score += 30;
        else if (ageAtEnd > 65) score += 20;

        return Math.min(100, score);
    }

    private int getContractDuration(InsuranceContract contract) {
        if (contract.getStartDate() == null || contract.getEndDate() == null) return 1;
        long diff = contract.getEndDate().getTime() - contract.getStartDate().getTime();
        return (int) (diff / (1000L * 60 * 60 * 24 * 365));
    }

    private String scoreToLevel(double score) {
        if (score >= 80) return "TRES_ELEVE";
        if (score >= 60) return "ELEVE";
        if (score >= 40) return "MODERE";
        if (score >= 20) return "FAIBLE";
        return "TRES_FAIBLE";
    }

    private String determineRiskClass(double score) {
        if (score >= 85) return "AAA";
        if (score >= 75) return "AA";
        if (score >= 65) return "A";
        if (score >= 55) return "BBB";
        if (score >= 45) return "BB";
        if (score >= 35) return "B";
        if (score >= 25) return "CCC";
        return "D";
    }

    private String generateRecommendation(RiskClaim riskClaim) {
        switch (riskClaim.getRiskLevel()) {
            case "HIGH":
                return "🔴 REJET RECOMMANDÉ - Risque trop élevé pour une acceptation";
            case "MEDIUM":
                return "🟡 ANALYSE REQUISE - Examiner les points de vigilance avant décision";
            case "LOW":
                return "🟢 ACCEPTATION - Risque maîtrisé, contrat acceptable";
            default:
                return "⚪ INFORMATION - Analyser le dossier manuellement";
        }
    }

    private String formatDate(Date date) {
        return date != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(date) : "N/A";
    }
}