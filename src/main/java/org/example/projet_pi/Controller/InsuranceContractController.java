package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IInsuranceContractService;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.entity.ContractStatus;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/contrats")
public class InsuranceContractController {

    private final IInsuranceContractService contractService;
    private final InsuranceContractRepository contractRepository; // Ajouté

    @PostMapping("/addCont")
    public InsuranceContractDTO addContract(@RequestBody InsuranceContractDTO dto) {
        return contractService.addContract(dto);
    }

    @PutMapping("/updateCont")
    public InsuranceContractDTO updateContract(@RequestBody InsuranceContractDTO dto) {
        return contractService.updateContract(dto);
    }

    @DeleteMapping("/deleteCont/{id}")
    public void deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
    }

    @GetMapping("/getCont/{id}")
    public InsuranceContractDTO getContractById(@PathVariable Long id) {
        return contractService.getContractById(id);
    }

    @GetMapping("/allCont")
    public List<InsuranceContractDTO> getAllContracts() {
        return contractService.getAllContracts();
    }

    // 🔥 NOUVEAU: Activer un contrat
    @PutMapping("/activate/{id}")
    public InsuranceContractDTO activateContract(@PathVariable Long id) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        if (contract.getStatus() == ContractStatus.INACTIVE) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract = contractRepository.save(contract);
        } else {
            throw new RuntimeException("Seuls les contrats INACTIVE peuvent être activés");
        }

        return InsuranceContractMapper.toDTO(contract);
    }

    // 🔥 NOUVEAU: Vérifier tous les contrats
    @PostMapping("/check-late-payments")
    public String checkAllLatePayments() {
        contractService.checkLatePayments();
        return "Vérification des retards effectuée pour tous les contrats";
    }

    // 🔥 NOUVEAU: Vérifier un contrat spécifique
    @PostMapping("/check-late-payments/{id}")
    public String checkContractLatePayments(@PathVariable Long id) {
        contractService.checkContractLatePayments(id);
        return "Vérification du contrat " + id + " effectuée";
    }

    // 🔥 NOUVEAU: Simuler des retards (pour tests)
    @PostMapping("/simulate-late-payments/{id}/{months}")
    public String simulateLatePayments(@PathVariable Long id, @PathVariable int months) {
        contractService.simulateLatePayments(id, months);
        return months + " mois de retard simulés pour le contrat " + id;
    }

    @PostMapping("/check-completed")
    public String checkCompletedContracts() {
        contractService.checkCompletedContracts();
        return "Vérification des contrats COMPLETED effectuée";
    }

    // 🔥 NOUVEAU: Vérifier la fin de mois manuellement
    @PostMapping("/check-end-of-month")
    public String checkEndOfMonth() {
        contractService.checkEndOfMonthLatePayments();
        return "Vérification de fin de mois effectuée";
    }
}