package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
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
    private Client client;

    @ManyToOne
    private InsuranceContract contract;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL)
    private Compensation compensation;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL)
    private List<Document> documents= new ArrayList<>();



}
