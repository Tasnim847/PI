package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class AgentAssurance extends User {

    @OneToMany(mappedBy = "agentAssurance")
    private List<InsuranceContract> contracts;

    @OneToMany(mappedBy = "agentAssurance")
    private List<Client> clients;

    @OneToMany(mappedBy = "agentAssurance")
    private List<Complaint> complaints;
}
