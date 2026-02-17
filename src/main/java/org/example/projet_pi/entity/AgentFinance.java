package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.List;

@Entity
public class AgentFinance extends User {

    @OneToMany(mappedBy = "agentFinance")
    private List<Client> clients;


    @OneToMany(mappedBy = "agentFinance")
    private List<Credit> credits;

    @OneToMany(mappedBy = "agentFinance")
    private List<Complaint> complaints;
}
