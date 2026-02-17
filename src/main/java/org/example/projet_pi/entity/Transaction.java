package org.example.projet_pi.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    private LocalDate date;

    private double amount;

    private String type;

    @ManyToOne
    private Account account;
}

