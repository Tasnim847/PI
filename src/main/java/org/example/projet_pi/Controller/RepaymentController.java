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

    @GetMapping("/allRepayment")
    public List<Repayment> getAllRepayments() {
        return repaymentService.getAllRepayments();
    }
}
