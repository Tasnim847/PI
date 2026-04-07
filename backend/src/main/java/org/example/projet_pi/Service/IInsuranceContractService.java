package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.InsuranceContractDTO;
import java.util.List;

public interface IInsuranceContractService {

    // Méthodes avec userEmail pour la sécurité
    InsuranceContractDTO addContract(InsuranceContractDTO dto, String userEmail);

    InsuranceContractDTO updateContract(Long contractId, InsuranceContractDTO dto, String userEmail);

    void deleteContract(Long id, String userEmail);

    InsuranceContractDTO getContractById(Long id, String userEmail);

    List<InsuranceContractDTO> getAllContracts(String userEmail);

    // Méthodes d'activation
    InsuranceContractDTO activateContract(Long contractId, String agentEmail);

    // Méthodes de vérification (sans userEmail car ce sont des tâches système)
    void checkLatePayments();

    void checkContractLatePayments(Long contractId);

    void simulateLatePayments(Long contractId, int monthsToAdd);

    void checkEndOfMonthLatePayments();

    void checkCompletedContracts();

    List<InsuranceContractDTO> getContractsByClientEmail(String email);

    InsuranceContractDTO rejectContract(Long contractId, String agentEmail, String rejectionReason);

}