package org.example.projet_pi.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Client extends User {

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

    @ManyToOne
    @JsonBackReference("agentAssurance-clients")
    private AgentAssurance agentAssurance;

    @ManyToOne
    @JsonBackReference("agentFinance-clients")
    private AgentFinance agentFinance;


    @OneToMany(mappedBy = "client")
    private java.util.List<Account> accounts;


    @OneToMany(mappedBy = "client")
    private java.util.List<Credit> credits;

    @OneToMany(mappedBy = "client")
    private java.util.List<InsuranceContract> contracts;

    @OneToMany(mappedBy = "client")
    private java.util.List<Complaint> complaints;

    @OneToMany(mappedBy = "client")
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Claim> claims = new ArrayList<>();
}
