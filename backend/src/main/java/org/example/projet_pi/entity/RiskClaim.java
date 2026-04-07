package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class RiskClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long riskId;

    private Double riskScore;      // score calculé
    private String riskLevel;

    @Column(length = 5000)
    private String evaluationNote;


    @OneToOne
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

}

