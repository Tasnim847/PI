package org.example.projet_pi.Service;

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
public class ClientScoringService {

    private final ClientRepository clientRepository;
    private final InsuranceContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final ClaimRepository claimRepository;

    // Poids des différentes composantes
    private static final double WEIGHT_DEMOGRAPHIC = 0.25;
    private static final double WEIGHT_FINANCIAL = 0.30;
    private static final double WEIGHT_BEHAVIORAL = 0.25;
    private static final double WEIGHT_HISTORICAL = 0.20;

    public ClientScoringService(
            ClientRepository clientRepository,
            InsuranceContractRepository contractRepository,
            PaymentRepository paymentRepository,
            ClaimRepository claimRepository) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.claimRepository = claimRepository;
    }

    /**
     * Calcule le score complet d'un client
     */
    @Transactional
    public ClientScoreResult calculateClientScore(Long clientId) {
        log.info("📊 Début du calcul de score pour le client {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        Map<String, Double> componentScores = new HashMap<>();
        Map<String, String> recommendations = new HashMap<>();

        // 1. Score démographique
        double demographicScore = calculateDemographicScore(client);
        componentScores.put("demographic", demographicScore);

        // 2. Score financier
        double financialScore = calculateFinancialScore(client);
        componentScores.put("financial", financialScore);

        // 3. Score comportemental
        double behavioralScore = calculateBehavioralScore(client);
        componentScores.put("behavioral", behavioralScore);

        // 4. Score historique
        double historicalScore = calculateHistoricalScore(client);
        componentScores.put("historical", historicalScore);

        // Calcul du score global pondéré
        double globalScore =
                demographicScore * WEIGHT_DEMOGRAPHIC +
                        financialScore * WEIGHT_FINANCIAL +
                        behavioralScore * WEIGHT_BEHAVIORAL +
                        historicalScore * WEIGHT_HISTORICAL;

        // Détermination du niveau de risque
        String riskLevel = determineRiskLevel(globalScore);
        String riskClass = determineRiskClass(globalScore);

        // Génération des recommandations
        generateRecommendations(client, componentScores, recommendations);

        // Mise à jour du client
        client.setCurrentRiskScore(globalScore);
        client.setCurrentRiskLevel(riskLevel);
        client.setLastScoringDate(new Date());
        clientRepository.save(client);

        log.info("✅ Score calculé pour client {}: {}/100 - Niveau: {}",
                clientId, String.format("%.2f", globalScore), riskLevel);

        return ClientScoreResult.builder()
                .clientId(clientId)
                .globalScore(globalScore)
                .riskLevel(riskLevel)
                .riskClass(riskClass)
                .componentScores(componentScores)
                .recommendations(recommendations)
                .calculationDate(new Date())
                .build();
    }

    /**
     * Score démographique (âge, situation familiale, logement, éducation)
     */
    private double calculateDemographicScore(Client client) {
        double score = 60.0; // Score de base

        // Âge (0-20 points)
        int age = client.getAge();
        if (age >= 35 && age <= 55) {
            score += 20; // Âge optimal
        } else if (age >= 25 && age < 35) {
            score += 15; // Jeune adulte
        } else if (age > 55 && age <= 65) {
            score += 15; // Pré-retraite
        } else if (age > 65) {
            score += 10; // Retraité
        } else if (age < 25) {
            score += 5; // Très jeune
        }

        // Situation familiale (0-15 points)
        String maritalStatus = client.getMaritalStatus();
        if ("MARIE".equalsIgnoreCase(maritalStatus)) {
            score += 15;
        } else if ("VEUF".equalsIgnoreCase(maritalStatus)) {
            score += 10;
        } else if ("DIVORCE".equalsIgnoreCase(maritalStatus)) {
            score += 5;
        } else {
            score += 5; // Célibataire
        }

        // Nombre de personnes à charge (0-10 points)
        Integer dependents = client.getNumberOfDependents();
        if (dependents != null) {
            if (dependents == 0) {
                score += 5;
            } else if (dependents <= 2) {
                score += 10;
            } else if (dependents <= 4) {
                score += 5;
            }
        }

        // Statut de logement (0-15 points)
        String housing = client.getHousingStatus();
        if ("PROPRIETAIRE".equalsIgnoreCase(housing)) {
            score += 15;
        } else if ("LOCATAIRE".equalsIgnoreCase(housing)) {
            score += 8;
        } else {
            score += 3; // Hébergé
        }

        // Niveau d'éducation (0-10 points)
        String education = client.getEducationLevel();
        if ("SUPERIEUR".equalsIgnoreCase(education)) {
            score += 10;
        } else if ("SECONDAIRE".equalsIgnoreCase(education)) {
            score += 5;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Score financier (revenus, profession, stabilité)
     */
    private double calculateFinancialScore(Client client) {
        double score = 50.0;

        // Revenus annuels (0-25 points)
        Double income = client.getAnnualIncome();
        if (income != null) {
            if (income > 50000) {
                score += 25;
            } else if (income > 30000) {
                score += 20;
            } else if (income > 20000) {
                score += 15;
            } else if (income > 10000) {
                score += 10;
            } else {
                score += 5;
            }
        }

        // Statut d'emploi (0-20 points)
        String employment = client.getEmploymentStatus();
        if ("CDI".equalsIgnoreCase(employment) || "FONCTIONNAIRE".equalsIgnoreCase(employment)) {
            score += 20;
        } else if ("INDEPENDANT".equalsIgnoreCase(employment)) {
            score += 15;
        } else if ("CDD".equalsIgnoreCase(employment)) {
            score += 10;
        } else if ("RETRAITE".equalsIgnoreCase(employment)) {
            score += 15;
        } else {
            score += 5; // Autre
        }

        // Profession (0-15 points)
        String profession = client.getProfession();
        if (profession != null) {
            // Liste des professions à risque faible
            List<String> lowRiskProfessions = Arrays.asList("MEDECIN", "AVOCAT", "INGENIEUR", "CADRE");
            // Liste des professions à risque élevé
            List<String> highRiskProfessions = Arrays.asList("COMMERCANT", "CHAUFFEUR", "OUVRIER");

            if (lowRiskProfessions.contains(profession.toUpperCase())) {
                score += 15;
            } else if (highRiskProfessions.contains(profession.toUpperCase())) {
                score += 5;
            } else {
                score += 10;
            }
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Score comportemental (ancienneté, activité, fidélité)
     */
    private double calculateBehavioralScore(Client client) {
        double score = 50.0;

        // Ancienneté (0-25 points)
        long tenureDays = client.getClientTenureInDays();
        if (tenureDays > 365 * 5) {
            score += 25; // 5+ ans
        } else if (tenureDays > 365 * 3) {
            score += 20; // 3-5 ans
        } else if (tenureDays > 365 * 1) {
            score += 15; // 1-3 ans
        } else if (tenureDays > 180) {
            score += 10; // 6-12 mois
        } else {
            score += 5; // Nouveau client
        }

        // Nombre de contrats (0-20 points)
        int contractCount = client.getContracts() != null ? client.getContracts().size() : 0;
        if (contractCount >= 3) {
            score += 20; // Multi-équipé
        } else if (contractCount == 2) {
            score += 15;
        } else if (contractCount == 1) {
            score += 10;
        }

        // Dernière activité (0-15 points)
        Date lastActivity = client.getLastActivityDate();
        if (lastActivity != null) {
            long daysSinceActivity = ChronoUnit.DAYS.between(
                    lastActivity.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
            );

            if (daysSinceActivity < 30) {
                score += 15; // Actif récemment
            } else if (daysSinceActivity < 90) {
                score += 10;
            } else if (daysSinceActivity < 180) {
                score += 5;
            }
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Score historique (paiements, sinistres, réclamations)
     */
    private double calculateHistoricalScore(Client client) {
        double score = 70.0; // Score de base

        // Historique des paiements
        List<Payment> payments = paymentRepository.findByContract_ClientId(client.getId());
        if (!payments.isEmpty()) {
            long latePayments = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.LATE)
                    .count();
            long failedPayments = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                    .count();

            // Pénalités (0-30 points)
            double latePenalty = (latePayments * 5.0);
            double failedPenalty = (failedPayments * 15.0);
            score -= Math.min(30, latePenalty + failedPenalty);
        }

        // Historique des sinistres
        List<Claim> claims = claimRepository.findByClientId(client.getId());
        if (!claims.isEmpty()) {
            long approvedClaims = claims.stream()
                    .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                    .count();
            long rejectedClaims = claims.stream()
                    .filter(c -> c.getStatus() == ClaimStatus.REJECTED)
                    .count();

            // Pénalités pour sinistres (0-40 points)
            double claimsPenalty = (approvedClaims * 5.0) + (rejectedClaims * 20.0);
            score -= Math.min(40, claimsPenalty);
        }

        // Historique des réclamations (complaints)
        List<Complaint> complaints = client.getComplaints();
        if (complaints != null && !complaints.isEmpty()) {
            long resolvedComplaints = complaints.stream()
                    .filter(c -> "RESOLVED".equals(c.getStatus()))
                    .count();
            long pendingComplaints = complaints.size() - resolvedComplaints;

            score -= (pendingComplaints * 10.0);
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Détermine le niveau de risque
     */
    private String determineRiskLevel(double score) {
        if (score >= 80) return "TRES_FAIBLE";
        if (score >= 65) return "FAIBLE";
        if (score >= 50) return "MODERE";
        if (score >= 35) return "ELEVE";
        return "TRES_ELEVE";
    }

    /**
     * Détermine la classe de risque (notation)
     */
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

    /**
     * Génère des recommandations basées sur les scores
     */
    private void generateRecommendations(Client client, Map<String, Double> scores, Map<String, String> recommendations) {
        if (scores.get("demographic") < 50) {
            recommendations.put("demographic", "Mettre à jour les informations démographiques");
        }

        if (scores.get("financial") < 40) {
            recommendations.put("financial", "Vérifier la capacité financière");
            if (client.getAnnualIncome() == null || client.getAnnualIncome() < 10000) {
                recommendations.put("income", "Revenus insuffisants - Envisager une réduction des garanties");
            }
        }

        if (scores.get("behavioral") < 45) {
            recommendations.put("behavioral", "Proposer un programme de fidélité");
        }

        if (scores.get("historical") < 40) {
            recommendations.put("historical", "Historique défavorable - Surveillance renforcée");

            // Vérifier les paiements en retard
            List<Payment> latePayments = paymentRepository.findLatePaymentsByClientId(client.getId());
            if (!latePayments.isEmpty()) {
                recommendations.put("payments", "Mettre en place un prélèvement automatique");
            }
        }
    }

    public List<ClaimScoreDTO> getClaimsScoreAdvanced(Long clientId) {

        List<Claim> claims = claimRepository.findByClientId(clientId);

        // Calcul fréquence temporelle
        Map<Long, Long> claimsByMonth = claims.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getClaimDate().getTime() / (1000 * 60 * 60 * 24 * 30),
                        Collectors.counting()
                ));

        return claims.stream().map(claim -> {

            int score = 0;
            StringBuilder delayInfo = new StringBuilder();
            StringBuilder docInfo = new StringBuilder();
            StringBuilder freqInfo = new StringBuilder();

            // 1️⃣ Montant élevé
            if (claim.getClaimedAmount() > 10000) score += 30;

            // 2️⃣ Peu de documents
            int docCount = claim.getDocuments() != null ? claim.getDocuments().size() : 0;
            if (docCount < 2) score += 20;
            docInfo.append("Documents: ").append(docCount);

            // 3️⃣ Délai entre contrat et claim
            if (claim.getContract() != null && claim.getClaimDate() != null) {
                long days = (claim.getClaimDate().getTime() - claim.getContract().getStartDate().getTime()) / (1000 * 60 * 60 * 24);
                if (days < 30) score += 15; // très rapide = suspect
                delayInfo.append(days).append(" jours après début contrat");
            }

            // 4️⃣ Historique client (trop de claims)
            if (claims.size() > 5) score += 15;

            // 5️⃣ Fréquence temporelle (plusieurs claims dans un mois)
            long monthCount = claimsByMonth.getOrDefault(claim.getClaimDate().getTime() / (1000 * 60 * 60 * 24 * 30), 0L);
            if (monthCount > 2) score += 20;
            freqInfo.append(monthCount).append(" claim(s) ce mois");

            // Niveau de risque
            String level;
            String color;
            if (score >= 60) {
                level = "HIGH"; color = "🔴";
            } else if (score >= 30) {
                level = "MEDIUM"; color = "🟡";
            } else {
                level = "LOW"; color = "🟢";
            }

            // Recommendation intelligente
            String recommendation;
            if (score >= 60) recommendation = "🚨 Vérification approfondie requise";
            else if (score >= 30) recommendation = "⚠️ Examiner le claim";
            else recommendation = "✅ Claim normal";

            return ClaimScoreDTO.builder()
                    .claimId(claim.getClaimId())
                    .claimedAmount(claim.getClaimedAmount())
                    .riskScore(score)
                    .riskLevel(level)
                    .recommendation(recommendation)
                    .colorCode(color)
                    .delayInfo(delayInfo.toString())
                    .documentTypeInfo(docInfo.toString())
                    .frequencyInfo(freqInfo.toString())
                    .build();

        }).toList();
    }
}