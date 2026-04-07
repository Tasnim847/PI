package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponseDTO {
    private String clientSecret;
    private String paymentIntentId;
    private Long contractId;
    private Double amount;
    private Double remainingAmount;
    private String status;
}