package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.entity.Payment;

public class PaymentMapper {

    public static PaymentDTO toDTO(Payment payment) {

        if (payment == null) return null;

        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setStatus(payment.getStatus().name());

        if (payment.getContract() != null)
            dto.setContractId(payment.getContract().getContractId());

        return dto;
    }

    public static Payment toEntity(PaymentDTO dto) {

        if (dto == null) return null;

        Payment payment = new Payment();
        payment.setPaymentId(dto.getPaymentId());
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());

        if (dto.getPaymentMethod() != null)
            payment.setPaymentMethod(Enum.valueOf(
                    org.example.projet_pi.entity.PaymentMethod.class,
                    dto.getPaymentMethod()
            ));

        if (dto.getStatus() != null)
            payment.setStatus(Enum.valueOf(
                    org.example.projet_pi.entity.PaymentStatus.class,
                    dto.getStatus()
            ));

        return payment;
    }
}