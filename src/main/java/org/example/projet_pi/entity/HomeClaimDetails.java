package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class HomeClaimDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String damageType; // fire, water...
    private String address;
    private Double estimatedLoss;

    @OneToOne
    private Claim claim;
}