package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Id;

import java.util.Date;
import java.util.List;

@Entity
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    @Temporal(TemporalType.DATE)
    private Date claimDate;

    private double claimedAmount;
    private double approvedAmount;

    private String description;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @ManyToOne
    private InsuranceContract contract;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL)
    private Compensation compensation;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL)
    private List<Document> documents;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL)
    private RiskClaim riskClaim;

    public RiskClaim getRiskClaim() {
        return riskClaim;
    }

    public void setRiskClaim(RiskClaim riskClaim) {
        this.riskClaim = riskClaim;
    }

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public Date getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(Date claimDate) {
        this.claimDate = claimDate;
    }

    public double getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(double approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public double getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(double claimedAmount) {
        this.claimedAmount = claimedAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public InsuranceContract getContract() {
        return contract;
    }

    public void setContract(InsuranceContract contract) {
        this.contract = contract;
    }

    public Compensation getCompensation() {
        return compensation;
    }

    public void setCompensation(Compensation compensation) {
        this.compensation = compensation;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }
}
