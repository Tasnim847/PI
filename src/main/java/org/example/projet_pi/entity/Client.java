package org.example.projet_pi.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Client extends User {


    @ManyToOne
    private AgentAssurance agentAssurance;

    @ManyToOne
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
}

