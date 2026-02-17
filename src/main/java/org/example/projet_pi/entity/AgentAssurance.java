package org.example.projet_pi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class AgentAssurance extends User {
    public List<InsuranceContract> getContracts() {
        return contracts;
    }

    public void setContracts(List<InsuranceContract> contracts) {
        this.contracts = contracts;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }

    public void setComplaints(List<Complaint> complaints) {
        this.complaints = complaints;
    }

    @OneToMany(mappedBy = "agentAssurance")
    @JsonIgnore
    private List<InsuranceContract> contracts;

    @OneToMany(mappedBy = "agentAssurance")
    @JsonManagedReference("agentAssurance-clients")
    private List<Client> clients;

    @OneToMany(mappedBy = "agentAssurance")
    @JsonIgnore
    private List<Complaint> complaints;
}
