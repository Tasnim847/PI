package org.example.projet_pi.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Client extends User {

    // 🔥 NOUVEAUX CHAMPS POUR LE SCORING
    private Date dateOfBirth;
    private String profession;
    private String employmentStatus; // CDI, CDD, INDEPENDANT, RETRAITE, etc.
    private Double annualIncome;
    private String maritalStatus; // MARIÉ, CÉLIBATAIRE, DIVORCÉ, VEUF
    private Integer numberOfDependents;
    private String educationLevel;
    private String housingStatus; // PROPRIETAIRE, LOCATAIRE, HEBERGE

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivityDate;

    // Score du client (mis à jour périodiquement)
    private Double currentRiskScore;
    private String currentRiskLevel;
    private Date lastScoringDate;

    // Relations existantes
    @ManyToOne
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private AgentAssurance agentAssurance;

    @ManyToOne
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private AgentFinance agentFinance;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private List<Credit> credits = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private List<InsuranceContract> contracts = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Complaint> complaints = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore  // ← Changé : plus de JsonManagedReference
    private List<Claim> claims = new ArrayList<>();

    // ============================================================
    // CONSTRUCTEURS
    // ============================================================

    public Client() {
        this.createdAt = new Date();
        this.lastActivityDate = new Date();
    }

    // ============================================================
    // GETTERS ET SETTERS POUR LES NOUVEAUX CHAMPS
    // ============================================================

    public AgentAssurance getAgentAssurance() {
        return agentAssurance;
    }

    public void setAgentAssurance(AgentAssurance agentAssurance) {
        this.agentAssurance = agentAssurance;
    }

    public AgentFinance getAgentFinance() {
        return agentFinance;
    }

    public void setAgentFinance(AgentFinance agentFinance) {
        this.agentFinance = agentFinance;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public List<Credit> getCredits() {
        return credits;
    }

    public void setCredits(List<Credit> credits) {
        this.credits = credits;
    }

    public List<InsuranceContract> getContracts() {
        return contracts;
    }

    public void setContracts(List<InsuranceContract> contracts) {
        this.contracts = contracts;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }

    public void setComplaints(List<Complaint> complaints) {
        this.complaints = complaints;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Double getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(Double annualIncome) {
        this.annualIncome = annualIncome;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public Integer getNumberOfDependents() {
        return numberOfDependents;
    }

    public void setNumberOfDependents(Integer numberOfDependents) {
        this.numberOfDependents = numberOfDependents;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getHousingStatus() {
        return housingStatus;
    }

    public void setHousingStatus(String housingStatus) {
        this.housingStatus = housingStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(Date lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Double getCurrentRiskScore() {
        return currentRiskScore;
    }

    public void setCurrentRiskScore(Double currentRiskScore) {
        this.currentRiskScore = currentRiskScore;
    }

    public String getCurrentRiskLevel() {
        return currentRiskLevel;
    }

    public void setCurrentRiskLevel(String currentRiskLevel) {
        this.currentRiskLevel = currentRiskLevel;
    }

    public Date getLastScoringDate() {
        return lastScoringDate;
    }

    public void setLastScoringDate(Date lastScoringDate) {
        this.lastScoringDate = lastScoringDate;
    }

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================

    public int getAge() {
        if (dateOfBirth == null) return 40; // Âge par défaut
        long diff = new Date().getTime() - dateOfBirth.getTime();
        return (int) (diff / (1000L * 60 * 60 * 24 * 365));
    }

    public long getClientTenureInDays() {
        if (createdAt == null) return 0;
        long diff = new Date().getTime() - createdAt.getTime();
        return diff / (1000 * 60 * 60 * 24);
    }

    // Récupérer tous les contrats actifs
    public List<InsuranceContract> getActiveContracts() {
        if (contracts == null) return new ArrayList<>();
        return contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .collect(java.util.stream.Collectors.toList());
    }

    // Calculer le total des primes
    public double getTotalPremiums() {
        if (contracts == null) return 0.0;
        return contracts.stream()
                .mapToDouble(InsuranceContract::getPremium)
                .sum();
    }

    // Compter les sinistres
    public long getTotalClaims() {
        if (claims == null) return 0;
        return claims.size();
    }

    // Compter les sinistres acceptés
    public long getApprovedClaims() {
        if (claims == null) return 0;
        return claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                .count();
    }
}