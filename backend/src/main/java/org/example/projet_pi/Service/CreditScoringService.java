package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.CreditScoreDTO;
import org.example.projet_pi.entity.Client;

public interface CreditScoringService {
    
    /**
     * Calcule le score de crédit pour un client donné par son ID
     * @param clientId ID du client
     * @return CreditScoreDTO contenant le score et l'analyse
     */
    CreditScoreDTO calculateCreditScore(Long clientId);
    
    /**
     * Calcule le score de crédit pour un client
     * @param client Le client
     * @return CreditScoreDTO contenant le score et l'analyse
     */
    CreditScoreDTO calculateCreditScore(Client client);
    
    /**
     * Analyse le profil du client avec Gemini AI
     * @param scoreData Les données de score du client
     * @return L'analyse textuelle de Gemini
     */
    String analyzeWithGemini(CreditScoreDTO scoreData);
}
