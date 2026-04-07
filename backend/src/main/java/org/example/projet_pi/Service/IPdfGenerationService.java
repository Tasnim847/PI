package org.example.projet_pi.Service;

import org.example.projet_pi.entity.*;

import java.io.OutputStream;
import java.util.List;

public interface IPdfGenerationService {

    /**
     * Génère un PDF du contrat
     * @param contract Le contrat
     * @param client Le client associé
     * @param agent L'agent d'assurance
     * @param payments La liste des paiements
     * @return byte[] Le contenu du PDF
     */
    byte[] generateContractPdf(InsuranceContract contract,
                               Client client,
                               AgentAssurance agent,
                               List<Payment> payments);

    /**
     * Génère un PDF du contrat en mode streaming
     * @param contract Le contrat
     * @param client Le client associé
     * @param agent L'agent d'assurance
     * @param payments La liste des paiements
     * @param outputStream Le flux de sortie
     */
    void generateContractPdfStreaming(InsuranceContract contract,
                                      Client client,
                                      AgentAssurance agent,
                                      List<Payment> payments,
                                      OutputStream outputStream);

    /**
     * Génère un PDF simplifié (pour les tests rapides)
     * @param contract Le contrat
     * @return byte[] Le contenu du PDF
     */
    byte[] generateSimpleContractPdf(InsuranceContract contract);
}