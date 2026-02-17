package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Payment;

import java.util.List;

public interface IPaymentService {

    //Iservice

    public Payment addPayment(Payment payment);

    public Payment updatePayment(Payment payment);

    public void deletePayment(Long id);

    public Payment getPaymentById(Long id);

    public List<Payment> getAllPayments();
}

