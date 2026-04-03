package org.example.projet_pi.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class HealthClaimDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientName;
    private String hospitalName;
    private String doctorName;

    private Double medicalCost;
    private String illnessType;

    @OneToOne
    private Claim claim;
}
