package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
public class AutoClaimDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String driverA;
    private String driverB;
    private String vehicleA;
    private String vehicleB;

    private String accidentLocation;
    private Date accidentDate;

    @OneToOne
    private Claim claim;
}