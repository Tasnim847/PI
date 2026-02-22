package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Service.IPaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final IPaymentService paymentService;

    // 🔥 Ajouter un paiement
    @PostMapping("/addPayment")
    public PaymentDTO addPayment(@RequestBody PaymentDTO dto) {
        return paymentService.addPayment(dto);
    }

    // 🔥 Modifier paiement (si autorisé)
    @PutMapping("/updatePayment")
    public PaymentDTO updatePayment(@RequestBody PaymentDTO dto) {
        return paymentService.updatePayment(dto);
    }

    // 🔥 Supprimer paiement
    @DeleteMapping("/deletePayment/{id}")
    public void deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
    }

    // 🔥 Récupérer par ID
    @GetMapping("/getPayment/{id}")
    public PaymentDTO getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    // 🔥 Tous les paiements
    @GetMapping("/allPayments")
    public List<PaymentDTO> getAllPayments() {
        return paymentService.getAllPayments();
    }
}