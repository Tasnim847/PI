package org.example.projet_pi.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.projet_pi.Dto.CreditScoreDTO;
import org.example.projet_pi.Dto.GeminiRequestDTO;
import org.example.projet_pi.Dto.GeminiResponseDTO;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.Repository.RepaymentRepository;
import org.example.projet_pi.config.GeminiConfig;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.example.projet_pi.entity.Repayment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class CreditScoringServiceImpl implements CreditScoringService {

    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private RepaymentRepository repaymentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private IRepaymentService repaymentService;

    @Autowired
    private GeminiConfig geminiConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public CreditScoreDTO calculateCreditScore(Long clientId) {
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty()) {
            throw new IllegalArgumentException("Client non trouvé avec l'ID: " + clientId);
        }
        return calculateCreditScore(clientOpt.get());
    }

    @Override
    public CreditScoreDTO calculateCreditScore(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Client ne peut pas être null");
        }
        
        CreditScoreDTO scoreDTO = new CreditScoreDTO(
            client.getId(),
            client.getFirstName() + " " + client.getLastName(),
            client.getEmail()
        );

        // Récupérer l'historique des crédits (filtrer ceux sans startDate car ils sont en PENDING)
        List<Credit> allCreditsList = creditRepository.findByClientId(client.getId());
        if (allCreditsList == null) {
            allCreditsList = List.of();
        }
        
        List<Credit> allCredits = allCreditsList.stream()
            .filter(credit -> credit != null && credit.getStartDate() != null)
            .toList();
            
        List<Credit> activeCreditsList = creditRepository.findByClientIdAndStatus(client.getId(), CreditStatus.IN_REPAYMENT);
        if (activeCreditsList == null) {
            activeCreditsList = List.of();
        }
        
        List<Credit> activeCredits = activeCreditsList.stream()
            .filter(credit -> credit != null && credit.getStartDate() != null)
            .toList();
            
        List<Credit> closedCreditsList = creditRepository.findByClientIdAndStatus(client.getId(), CreditStatus.CLOSED);
        if (closedCreditsList == null) {
            closedCreditsList = List.of();
        }
        
        List<Credit> closedCredits = closedCreditsList.stream()
            .filter(credit -> credit != null && credit.getStartDate() != null)
            .toList();

        // Calculer les métriques de base
        scoreDTO.setTotalCredits(allCredits.size());
        scoreDTO.setActiveCredits(activeCredits.size());
        scoreDTO.setClosedCredits(closedCredits.size());

        // Montant total des crédits
        Double totalAmount = allCredits.stream()
            .mapToDouble(Credit::getAmount)
            .sum();
        scoreDTO.setTotalAmount(BigDecimal.valueOf(totalAmount));

        // Dette actuelle
        Double currentDebt = activeCredits.stream()
            .mapToDouble(credit -> {
                try {
                    return repaymentService.getRemainingAmount(credit.getCreditId()).doubleValue();
                } catch (Exception e) {
                    return credit.getAmount();
                }
            })
            .sum();
        scoreDTO.setCurrentDebt(BigDecimal.valueOf(currentDebt));

        // Paiement mensuel moyen
        Double averageMonthlyPayment = activeCredits.stream()
            .mapToDouble(Credit::getMonthlyPayment)
            .average()
            .orElse(0.0);
        scoreDTO.setAverageMonthlyPayment(averageMonthlyPayment);

        // Pourcentage de retard moyen
        if (!closedCredits.isEmpty()) {
            double totalLatePercentage = closedCredits.stream()
                .mapToDouble(credit -> {
                    try {
                        // Vérifier si le crédit a été remboursé après la date d'échéance
                        List<Repayment> repayments = repaymentService.getRepaymentsByCreditId(credit.getCreditId());
                        if (repayments.isEmpty() || credit.getDueDate() == null) {
                            return 0.0;
                        }
                        
                        // Trouver la date du dernier paiement
                        LocalDate lastPaymentDate = repayments.stream()
                            .map(Repayment::getPaymentDate)
                            .filter(date -> date != null)
                            .max(LocalDate::compareTo)
                            .orElse(null);
                        
                        if (lastPaymentDate == null) {
                            return 0.0;
                        }
                        
                        // Si le dernier paiement est après la date d'échéance, calculer le retard
                        if (lastPaymentDate.isAfter(credit.getDueDate())) {
                            long daysLate = ChronoUnit.DAYS.between(credit.getDueDate(), lastPaymentDate);
                            
                            // Vérifier que startDate n'est pas null avant de l'utiliser
                            if (credit.getStartDate() == null) {
                                return 0.0;
                            }
                            
                            // Convertir startDate (java.sql.Date) en LocalDate via getTime()
                            LocalDate startLocalDate = new java.util.Date(credit.getStartDate().getTime()).toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            
                            long totalDays = ChronoUnit.DAYS.between(startLocalDate, lastPaymentDate);
                            return totalDays > 0 ? (daysLate * 100.0) / totalDays : 0.0;
                        }
                        
                        return 0.0;
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .sum();
            scoreDTO.setAverageLatePercentage(totalLatePercentage / closedCredits.size());
        } else {
            scoreDTO.setAverageLatePercentage(0.0);
        }

        // Jours depuis le dernier crédit
        if (!allCredits.isEmpty()) {
            LocalDate lastCreditDate = allCredits.stream()
                .map(credit -> {
                    // Convertir java.sql.Date en LocalDate via getTime()
                    return new java.util.Date(credit.getStartDate().getTime()).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                })
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
            scoreDTO.setDaysSinceLastCredit(ChronoUnit.DAYS.between(lastCreditDate, LocalDate.now()));
        } else {
            scoreDTO.setDaysSinceLastCredit(0L);
        }

        // Analyser avec Gemini AI
        String geminiAnalysis = analyzeWithGemini(scoreDTO);
        parseGeminiResponse(scoreDTO, geminiAnalysis);

        return scoreDTO;
    }

    @Override
    public String analyzeWithGemini(CreditScoreDTO scoreData) {
        try {
            System.out.println("=== DÉBUT APPEL GEMINI API ===");
            System.out.println("API URL: " + geminiConfig.getGeminiApiUrl());
            System.out.println("API Key présente: " + (geminiConfig.getGeminiApiKey() != null && !geminiConfig.getGeminiApiKey().isEmpty()));
            
            String prompt = buildAnalysisPrompt(scoreData);
            System.out.println("Prompt construit: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");
            
            // Construire la requête Gemini
            GeminiRequestDTO.Part part = new GeminiRequestDTO.Part(prompt);
            GeminiRequestDTO.Content content = new GeminiRequestDTO.Content(Arrays.asList(part));
            GeminiRequestDTO request = new GeminiRequestDTO(Arrays.asList(content));

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GeminiRequestDTO> entity = new HttpEntity<>(request, headers);

            // Appel API
            String url = geminiConfig.getGeminiApiUrl() + "?key=" + geminiConfig.getGeminiApiKey();
            System.out.println("URL complète (sans clé): " + geminiConfig.getGeminiApiUrl());
            
            ResponseEntity<GeminiResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, GeminiResponseDTO.class
            );

            System.out.println("Réponse reçue - Status: " + response.getStatusCode());
            
            if (response.getBody() != null && 
                response.getBody().getCandidates() != null && 
                !response.getBody().getCandidates().isEmpty()) {
                
                String analysisText = response.getBody().getCandidates().get(0)
                    .getContent().getParts().get(0).getText();
                System.out.println("Analyse Gemini reçue: " + analysisText.substring(0, Math.min(100, analysisText.length())) + "...");
                System.out.println("=== FIN APPEL GEMINI API (SUCCÈS) ===");
                return analysisText;
            }

            System.out.println("=== FIN APPEL GEMINI API (RÉPONSE VIDE) ===");
            return generateFallbackAnalysis(scoreData);

        } catch (Exception e) {
            System.err.println("=== ERREUR APPEL GEMINI API ===");
            System.err.println("Type d'erreur: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            System.err.println("=== UTILISATION DU FALLBACK ===");
            return generateFallbackAnalysis(scoreData);
        }
    }

    private String buildAnalysisPrompt(CreditScoreDTO scoreData) {
        return String.format("""
            Tu es un expert en analyse de crédit bancaire. Analyse le profil suivant et fournis un scoring de crédit.
            
            PROFIL CLIENT:
            - Nom: %s
            - Email: %s
            - Nombre total de crédits: %d
            - Crédits actifs: %d
            - Crédits fermés: %d
            - Montant total emprunté: %.2f TND
            - Dette actuelle: %.2f TND
            - Paiement mensuel moyen: %.2f TND
            - Pourcentage de retard moyen: %.2f%%
            - Jours depuis le dernier crédit: %d
            
            INSTRUCTIONS:
            1. Calcule un score de crédit entre 300 et 850 (300=très risqué, 850=excellent)
            2. Détermine le niveau de risque: FAIBLE, MOYEN, ÉLEVÉ, TRÈS_ÉLEVÉ
            3. Donne une recommandation: APPROUVER, APPROUVER_AVEC_CONDITIONS, REJETER
            4. Fournis une analyse détaillée des forces et faiblesses
            
            RÉPONSE REQUISE (format exact):
            SCORE: [nombre entre 300-850]
            RISQUE: [FAIBLE/MOYEN/ÉLEVÉ/TRÈS_ÉLEVÉ]
            RECOMMANDATION: [APPROUVER/APPROUVER_AVEC_CONDITIONS/REJETER]
            ANALYSE: [analyse détaillée en français, 2-3 phrases]
            """,
            scoreData.getClientName(),
            scoreData.getClientEmail(),
            scoreData.getTotalCredits() != null ? scoreData.getTotalCredits() : 0,
            scoreData.getActiveCredits() != null ? scoreData.getActiveCredits() : 0,
            scoreData.getClosedCredits() != null ? scoreData.getClosedCredits() : 0,
            scoreData.getTotalAmount() != null ? scoreData.getTotalAmount().doubleValue() : 0.0,
            scoreData.getCurrentDebt() != null ? scoreData.getCurrentDebt().doubleValue() : 0.0,
            scoreData.getAverageMonthlyPayment() != null ? scoreData.getAverageMonthlyPayment() : 0.0,
            scoreData.getAverageLatePercentage() != null ? scoreData.getAverageLatePercentage() : 0.0,
            scoreData.getDaysSinceLastCredit() != null ? scoreData.getDaysSinceLastCredit() : 0
        );
    }

    private void parseGeminiResponse(CreditScoreDTO scoreDTO, String geminiResponse) {
        try {
            String[] lines = geminiResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                if (line.startsWith("SCORE:")) {
                    String scoreStr = line.substring(6).trim();
                    scoreDTO.setScore(Integer.parseInt(scoreStr.replaceAll("[^0-9]", "")));
                }
                else if (line.startsWith("RISQUE:")) {
                    scoreDTO.setRiskLevel(line.substring(7).trim());
                }
                else if (line.startsWith("RECOMMANDATION:")) {
                    scoreDTO.setRecommendation(line.substring(15).trim());
                }
                else if (line.startsWith("ANALYSE:")) {
                    scoreDTO.setAnalysis(line.substring(8).trim());
                }
            }

            // Validation et valeurs par défaut
            if (scoreDTO.getScore() == null || scoreDTO.getScore() < 300 || scoreDTO.getScore() > 850) {
                scoreDTO.setScore(calculateFallbackScore(scoreDTO));
            }
            
            if (scoreDTO.getRiskLevel() == null) {
                scoreDTO.setRiskLevel(determineRiskLevel(scoreDTO.getScore()));
            }
            
            if (scoreDTO.getRecommendation() == null) {
                scoreDTO.setRecommendation(determineRecommendation(scoreDTO.getScore()));
            }
            
            if (scoreDTO.getAnalysis() == null || scoreDTO.getAnalysis().isEmpty()) {
                scoreDTO.setAnalysis("Analyse basée sur l'historique de crédit et les métriques de paiement.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de la réponse Gemini: " + e.getMessage());
            // Utiliser l'analyse de fallback
            scoreDTO.setScore(calculateFallbackScore(scoreDTO));
            scoreDTO.setRiskLevel(determineRiskLevel(scoreDTO.getScore()));
            scoreDTO.setRecommendation(determineRecommendation(scoreDTO.getScore()));
            scoreDTO.setAnalysis(generateFallbackAnalysis(scoreDTO));
        }
    }

    private String generateFallbackAnalysis(CreditScoreDTO scoreData) {
        int score = calculateFallbackScore(scoreData);
        scoreData.setScore(score);
        scoreData.setRiskLevel(determineRiskLevel(score));
        scoreData.setRecommendation(determineRecommendation(score));

        StringBuilder analysis = new StringBuilder();
        
        if (scoreData.getAverageLatePercentage() != null && scoreData.getAverageLatePercentage() > 20) {
            analysis.append("Historique de retards préoccupant (").append(String.format("%.1f", scoreData.getAverageLatePercentage())).append("%). ");
        } else {
            analysis.append("Bon historique de paiement. ");
        }

        if (scoreData.getCurrentDebt() != null && scoreData.getCurrentDebt().compareTo(BigDecimal.valueOf(50000)) > 0) {
            analysis.append("Dette actuelle élevée. ");
        }

        if (scoreData.getTotalCredits() != null && scoreData.getTotalCredits() > 5) {
            analysis.append("Expérience significative avec les crédits. ");
        } else if (scoreData.getTotalCredits() != null && scoreData.getTotalCredits() == 0) {
            analysis.append("Nouveau client sans historique. ");
        }

        return analysis.toString().trim();
    }

    private Integer calculateFallbackScore(CreditScoreDTO scoreData) {
        int baseScore = 650; // Score de base

        // Ajustements basés sur l'historique
        if (scoreData.getAverageLatePercentage() != null) {
            baseScore -= (int) (scoreData.getAverageLatePercentage() * 2); // -2 points par % de retard
        }

        // Bonus pour l'expérience
        if (scoreData.getClosedCredits() != null && scoreData.getClosedCredits() > 0) {
            baseScore += Math.min(scoreData.getClosedCredits() * 10, 50); // +10 points par crédit fermé, max 50
        }

        // Pénalité pour dette élevée
        if (scoreData.getCurrentDebt() != null && scoreData.getCurrentDebt().compareTo(BigDecimal.valueOf(100000)) > 0) {
            baseScore -= 100;
        }

        // Nouveau client
        if (scoreData.getTotalCredits() != null && scoreData.getTotalCredits() == 0) {
            baseScore = 600; // Score neutre pour nouveau client
        }

        return Math.max(300, Math.min(850, baseScore));
    }

    private String determineRiskLevel(Integer score) {
        if (score >= 750) return "FAIBLE";
        if (score >= 650) return "MOYEN";
        if (score >= 550) return "ÉLEVÉ";
        return "TRÈS_ÉLEVÉ";
    }

    private String determineRecommendation(Integer score) {
        if (score >= 700) return "APPROUVER";
        if (score >= 600) return "APPROUVER_AVEC_CONDITIONS";
        return "REJETER";
    }
}