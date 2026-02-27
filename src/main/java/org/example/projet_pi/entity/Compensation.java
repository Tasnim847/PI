package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
public class Compensation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long compensationId;

    private Double amount;

    @Temporal(TemporalType.DATE)
    private Date paymentDate;

    @OneToOne
    private Claim claim;
}
