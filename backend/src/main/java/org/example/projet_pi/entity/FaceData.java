package org.example.projet_pi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_data")
@Data
public class FaceData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User user;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] faceFeatures; // Stockage des features faciales

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    private LocalDateTime lastVerifiedAt;

    private String faceImagePath; // Chemin vers la photo du visage

    public FaceData() {}

    public FaceData(User user, byte[] faceFeatures) {
        this.user = user;
        this.faceFeatures = faceFeatures;
        this.registeredAt = LocalDateTime.now();
    }
}