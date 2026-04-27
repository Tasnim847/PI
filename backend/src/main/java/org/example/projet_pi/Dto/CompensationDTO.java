package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.projet_pi.entity.CompensationStatus;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompensationDTO {

    private Long compensationId;
    private Double amount; // Montant payé par l'assurance
    private Date paymentDate;
    private Long claimId;

    // NOUVEAUX CHAMPS
    private Double clientOutOfPocket; // Reste à charge client
    private Double coverageLimit; // Plafond du contrat
    private Double deductible; // Franchise
    private Double originalClaimedAmount; // Montant réclamé initial
    private Double approvedAmount; // Montant approuvé
    private String message; // Message explicatif
    private CompensationStatus status;
    private Date calculationDate;

    private Integer riskScore;
    private String riskLevel;
    private String decisionSuggestion;
    private String scoringDetails;
    private Double adjustedAmount;

    private ClientDTO client;

    // Constructeur simplifié pour la rétrocompatibilité
    public CompensationDTO(Long compensationId, Double amount, Date paymentDate, Long claimId) {
        this.compensationId = compensationId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.claimId = claimId;
    }
    public Double getInsurancePayment() {
        return this.amount; // amount représente le montant payé par l'assurance
    }
}