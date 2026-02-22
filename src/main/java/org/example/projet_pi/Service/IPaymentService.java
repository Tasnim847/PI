package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.PaymentDTO;

import java.util.List;

public interface IPaymentService {

    PaymentDTO addPayment(PaymentDTO dto);

    PaymentDTO updatePayment(PaymentDTO dto);

    void deletePayment(Long id);

    PaymentDTO getPaymentById(Long id);

    List<PaymentDTO> getAllPayments();
}