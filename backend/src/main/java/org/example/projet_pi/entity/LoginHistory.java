package org.example.projet_pi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Relation avec User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // ✅ Garder email aussi pour les cas OAuth2
    @Column(nullable = true)
    private String email;

    // ✅ loginTime (renommé depuis loginDate)
    @Column(name = "login_time")
    private Date loginTime;

    private String ipAddress;

    // Géolocalisation
    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    @Column(nullable = true)
    private String city;

    @Column(nullable = true)
    private String country;
}