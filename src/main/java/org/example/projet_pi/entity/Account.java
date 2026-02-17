package org.example.projet_pi.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    private double balance;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    private String status;

    @ManyToOne
    private Client client;

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;
}
