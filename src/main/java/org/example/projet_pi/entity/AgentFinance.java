package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;

@Entity
public class AgentFinance extends User {


    @OneToMany(mappedBy = "agentFinance")
    @JsonManagedReference("agentFinance-clients")
    private List<Client> clients;


    @OneToMany(mappedBy = "agentFinance")
    private List<Credit> credits;


    @OneToMany(mappedBy = "agentFinance")
    private List<Complaint> complaints;


    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public List<Credit> getCredits() {
        return credits;
    }

    public void setCredits(List<Credit> credits) {
        this.credits = credits;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }

    public void setComplaints(List<Complaint> complaints) {
        this.complaints = complaints;
    }
}

