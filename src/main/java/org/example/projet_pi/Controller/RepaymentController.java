package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.IRepaymentService;
import org.example.projet_pi.entity.Repayment;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Repayment")
public class RepaymentController {

    private final IRepaymentService repaymentService;

    public RepaymentController(IRepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping("/addRepayment")
    public Repayment addRepayment(@RequestBody Repayment repayment) {
        return repaymentService.addRepayment(repayment);
    }

    @PutMapping("/updateRepayment")
    public Repayment updateRepayment(@RequestBody Repayment repayment) {
        return repaymentService.updateRepayment(repayment);
    }

    @DeleteMapping("/deleteRepayment/{id}")
    public void deleteRepayment(@PathVariable Long id) {
        repaymentService.deleteRepayment(id);
    }

    // ✅ (optionnel mais utile) récupérer un repayment par id
    @GetMapping("/getRepayment/{id}")
    public Repayment getRepaymentById(@PathVariable Long id) {
        return repaymentService.getRepaymentById(id);
    }

    @GetMapping("/allRepayment")
    public List<Repayment> getAllRepayments() {
        return repaymentService.getAllRepayments();
    }

    // ✅ METIER : payer un crédit
    // Exemple: POST /Repayment/pay/5?allowPartialIfOverpay=true
    @PostMapping("/pay-credit/{creditId}")
    public Repayment payCredit(
            @PathVariable Long creditId,
            @RequestParam(name = "allowPartialIfOverpay", defaultValue = "true") boolean allowPartialIfOverpay,
            @RequestBody Repayment repayment
    ) {
        return repaymentService.payCredit(creditId, repayment, allowPartialIfOverpay);
    }
}