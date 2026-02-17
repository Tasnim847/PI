package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IPaymentService;
import org.example.projet_pi.entity.Payment;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    IPaymentService paymentService;

    @PostMapping("/addPayment")
    public Payment addPayment(@RequestBody Payment payment) {
        Payment newPayment = paymentService.addPayment(payment);
        return newPayment;
    }

    @PutMapping("/updatePayment")
    public Payment updatePayment(@RequestBody Payment payment) {
        Payment newPayment = paymentService.updatePayment(payment);
        return newPayment;
    }

    @DeleteMapping("/deletePayment/{id}")
    public void deletePayment(@PathVariable("id") Long id) {
        paymentService.deletePayment(id);
    }

    @GetMapping("/getPayment/{id}")
    public Payment getPaymentById(@PathVariable("id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        return payment;
    }

    @GetMapping("/allPayments")
    public List<Payment> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return payments;
    }
}
