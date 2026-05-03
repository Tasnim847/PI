package org.example.projet_pi.Dto;

import lombok.Data;

@Data
public class BankTransferPaymentRequestDTO {
    private String clientEmail;
    private Long contractId;
    private Double installmentAmount;
    private String paymentType;
    private Double remainingAmount;
    private String sourceRip;  // Spécifique au virement bancaire
}