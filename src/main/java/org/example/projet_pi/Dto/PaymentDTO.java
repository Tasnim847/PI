package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PaymentDTO {

    private Long paymentId;
    private double amount;
    private Date paymentDate;

    private String paymentMethod;
    private String status;

    private Long contractId;
}