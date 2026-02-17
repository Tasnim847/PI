package org.example.projet_pi.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
public class InsuranceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contractId;

    private Date startDate;
    private Date endDate;

    private double premium;          // prime du contrat
    private double deductible;       // franchise
    private double coverageLimit;    // plafond

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

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


    public double calculateCompensation(double claimedAmount) {
        double amountAfterDeductible = claimedAmount - deductible;
        if (amountAfterDeductible <= 0) return 0;
        return Math.min(amountAfterDeductible, coverageLimit);
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    public AgentAssurance getAgentAssurance() {
        return agentAssurance;
    }

    public void setAgentAssurance(AgentAssurance agentAssurance) {
        this.agentAssurance = agentAssurance;
    }

    public InsuranceProduct getProduct() {
        return product;
    }

    public void setProduct(InsuranceProduct product) {
        this.product = product;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public double getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(double coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public double getDeductible() {
        return deductible;
    }

    public void setDeductible(double deductible) {
        this.deductible = deductible;
    }

    public double getPremium() {
        return premium;
    }

    public void setPremium(double premium) {
        this.premium = premium;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }
}

