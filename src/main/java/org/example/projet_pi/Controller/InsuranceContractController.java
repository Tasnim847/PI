package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IInsuranceContractService;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/contrats")
public class InsuranceContractController {

    private final IInsuranceContractService contractService;

    @PostMapping("/add")
    public InsuranceContractDTO addContract(@RequestBody InsuranceContractDTO dto) {
        return contractService.addContract(dto);
    }

    @PutMapping("/update")
    public InsuranceContractDTO updateContract(@RequestBody InsuranceContractDTO dto) {
        return contractService.updateContract(dto);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
    }

    @GetMapping("/{id}")
    public InsuranceContractDTO getContractById(@PathVariable Long id) {
        return contractService.getContractById(id);
    }

    @GetMapping("/all")
    public List<InsuranceContractDTO> getAllContracts() {
        return contractService.getAllContracts();
    }
}