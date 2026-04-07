package org.example.projet_pi.entity;

public enum CompensationStatus {
    PENDING,     // En attente
    CALCULATED,  // Calculé mais pas encore payé
    PAID,        // Payé
    CANCELLED    // Annulé
}