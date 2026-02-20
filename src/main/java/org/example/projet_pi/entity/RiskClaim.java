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

    private double riskScore;      // score calculé
    private String riskLevel;
    private String evaluationNote;

    @OneToOne
    private Claim claim;

}

