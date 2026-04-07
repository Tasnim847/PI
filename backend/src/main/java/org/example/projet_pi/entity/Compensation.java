package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
public class Compensation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long compensationId;

    private Double amount; // Montant payé par l'assurance

    @Temporal(TemporalType.DATE)
    private Date paymentDate;

    // NOUVEAUX CHAMPS pour plus de transparence
    private Double clientOutOfPocket; // Reste à charge du client
    private Double coverageLimit; // Plafond du contrat au moment du calcul
    private Double deductible; // Franchise appliquée
    private Double originalClaimedAmount; // Montant initial réclamé
    private Double approvedAmount; // Montant approuvé

    @Column(length = 5000)
    private String message; // Message explicatif pour le client

    @Enumerated(EnumType.STRING)
    private CompensationStatus status;

    @OneToOne
    @JoinColumn(name = "claim_id")
    private Claim claim;

    // Dans Compensation.java - AJOUTER CES CHAMPS

    @Column(name = "risk_score")
    private Integer riskScore; // Score de risque du claim (0-100)

    @Column(name = "risk_level")
    private String riskLevel; // Niveau de risque (FAIBLE, MODERE, ELEVE, etc.)

    @Column(name = "decision_suggestion")
    private String decisionSuggestion; // AUTO_APPROVE, AUTO_REJECT, MANUAL_REVIEW

    @Column(name = "scoring_details", length = 1000)
    private String scoringDetails; // Détails complets du scoring

    @Column(name = "adjusted_amount")
    private Double adjustedAmount; // Montant ajusté après scoring (avec pénalité/bonus)

    // Date de calcul de la compensation
    @Temporal(TemporalType.TIMESTAMP)
    private Date calculationDate;

    // Méthode utilitaire pour calculer le reste à charge
    public void calculateClientOutOfPocket() {
        if (this.approvedAmount != null && this.amount != null) {
            this.clientOutOfPocket = this.approvedAmount - this.amount;
        }
    }
}