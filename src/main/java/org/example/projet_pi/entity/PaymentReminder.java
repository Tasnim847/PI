package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
public class PaymentReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sentDate;

    private Integer daysBefore; // 30, 15, 7, 3, ou 1

    private boolean sent;

    private String emailStatus; // SUCCESS, FAILED

    @PrePersist
    protected void onCreate() {
        sentDate = new Date();
    }
}