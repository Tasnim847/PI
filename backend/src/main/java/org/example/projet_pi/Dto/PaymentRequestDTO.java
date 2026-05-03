package org.example.projet_pi.Dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private String clientEmail;     // String pour l'email
    private Long contractId;        // Id du contrat
    private Double installmentAmount;
    private String paymentType;     // INSTALLMENT ou FULL
    private Double remainingAmount;
    private String sourceRip;       // 🔥 AJOUTER CETTE PROPRIÉTÉ pour le virement bancaire
}