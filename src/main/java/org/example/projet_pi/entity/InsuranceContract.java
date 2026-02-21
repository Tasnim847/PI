package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
public class InsuranceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contractId;

    private Date startDate;
    private Date endDate;

    private double premium;          // prime totale du contrat
    private double deductible;       // franchise
    private double coverageLimit;    // plafond

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    // ✅ Nouveau champ : fréquence de paiement
    @Enumerated(EnumType.STRING)
    private PaymentFrequency paymentFrequency;

    @ManyToOne
    private Client client;

    @ManyToOne
    private InsuranceProduct product;

    @ManyToOne
    private AgentAssurance agentAssurance;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<Claim> claims;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<Payment> payments;

    // Calculer le montant de compensation pour un claim
    public double calculateCompensation(double claimedAmount) {
        double amountAfterDeductible = claimedAmount - deductible;
        if (amountAfterDeductible <= 0) return 0;
        return Math.min(amountAfterDeductible, coverageLimit);
    }

    // ⚡ Logique métier avancée : calcul des échéances selon la fréquence de paiement
    public double calculateInstallmentAmount() {
        if (paymentFrequency == null) return premium; // par défaut montant total
        switch (paymentFrequency) {
            case MONTHLY:
                return premium / 12;
            case SEMI_ANNUAL:
                return premium / 2;
            case ANNUAL:
            default:
                return premium;
        }
    }
}