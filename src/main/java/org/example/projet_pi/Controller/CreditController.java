package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.CreditHistoryDTO;
import org.example.projet_pi.Service.CreditService;
import org.example.projet_pi.Service.IClientService;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Credit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.projet_pi.Dto.CreditHistoryWithAverageDTO;

import java.util.List;

@RestController
@RequestMapping("/Credit")
public class CreditController {

    private final CreditService creditService;
    @Autowired
    private IClientService clientService;


    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    // ===============================
    // CREATE
    // ===============================
    @PostMapping("/addCredit")
    public Credit addCredit(@RequestBody Credit credit) {
        return creditService.addCredit(credit);
    }

    // ===============================
    // APPROVE
    // ===============================
    @PutMapping("/approve/{id}")
    public Credit approveCredit(@PathVariable Long id,
                                @RequestParam double interestRate) {
        return creditService.approveCredit(id, interestRate);
    }

    // ===============================
    // REJECT
    // ===============================
    @PutMapping("/reject/{id}")
    public Credit rejectCredit(@PathVariable Long id) {
        return creditService.rejectCredit(id);
    }

    // ===============================
    // CRUD
    // ===============================
    @PutMapping("/updateCredit")
    public Credit updateCredit(@RequestBody Credit credit) {
        return creditService.updateCredit(credit);
    }

    @DeleteMapping("/deleteCredit/{id}")
    public void deleteCredit(@PathVariable Long id) {
        creditService.deleteCredit(id);
    }

    @GetMapping("/allCredit")
    public List<Credit> getAllCredits() {
        return creditService.getAllCredits();
    }

//    @GetMapping("/closedCredits/{clientId}")
//    public List<CreditHistoryDTO> getClosedCreditsByClient(@PathVariable Long clientId) {
//        Client client = clientService.getClientById(clientId); // Assure-toi d'avoir un ClientService
//        return creditService.getClosedCreditsWithLateRepaymentPercentage(client);
//    }
    @GetMapping("/closedCreditsWithAverage/{clientId}")
    public CreditHistoryWithAverageDTO getClosedCreditsWithAverage(@PathVariable Long clientId) {

        Client client = clientService.getClientById(clientId);

        if (client == null) {
            throw new RuntimeException("Client not found with id " + clientId);
        }

        return creditService.getClosedCreditsWithAverage(client);
    }
}