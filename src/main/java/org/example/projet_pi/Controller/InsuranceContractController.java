package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IInsuranceContractService;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/contrats")
public class InsuranceContractController {

    IInsuranceContractService insuranceContractService;

    //crude

    @PostMapping("/addContrat")
    public InsuranceContract addContract(@RequestBody InsuranceContract contract) {
        InsuranceContract contract1 = insuranceContractService.addContract(contract);
        return contract1;
    }

    @PutMapping("/updateContrat")
    public InsuranceContract updateContract(@RequestBody InsuranceContract contract) {
        InsuranceContract contract1 = insuranceContractService.updateContract(contract);
        return contract1;
    }

    @DeleteMapping("/deleteContrat/{id}")
    public void deleteContract(@PathVariable("id") Long id) {
        insuranceContractService.deleteContract(id);
    }

    @GetMapping("/getContrat/{id}")
    public InsuranceContract getContractById(@PathVariable("id") Long id) {
        InsuranceContract contract1 = insuranceContractService.getContractById(id);
        return contract1;
    }

    @GetMapping("/allContrat")
    public List<InsuranceContract> getAllContracts() {
        List<InsuranceContract> contracts = insuranceContractService.getAllContracts();
        return contracts;
    }
}
