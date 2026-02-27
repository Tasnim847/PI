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

    private Double premium;
    private Double deductible;
    private Double coverageLimit;

    // 🔥 NOUVEAUX CHAMPS
    private Double totalPaid = 0.0;
    private Double remainingAmount;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @OneToOne(mappedBy = "contract",  cascade = CascadeType.ALL, orphanRemoval = true)
    private RiskClaim riskClaim;

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

    // ============================================================
// 🔥 CALCUL DES ÉCHÉANCES SELON LA FRÉQUENCE
// ============================================================
    public double calculateInstallmentAmount() {

        if (paymentFrequency == null)
            return premium;

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

    // 🔥 INITIALISATION
    public void initializeAmounts() {
        this.totalPaid = 0.0;
        this.remainingAmount = this.premium;
    }

    // 🔥 LOGIQUE MÉTIER AVANCÉE
    public void applyPayment(double amount) {

        if (amount <= 0)
            throw new RuntimeException("Montant invalide");

        if (amount > remainingAmount)
            throw new RuntimeException("Paiement dépasse le montant restant");

        this.totalPaid += amount;
        this.remainingAmount -= amount;

        if (this.remainingAmount == 0) {
            this.status = ContractStatus.COMPLETED;
        }
    }
}