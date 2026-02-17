package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class InsuranceContractService implements IInsuranceContractService {
    InsuranceContractRepository insuranceContractRepository;
    //service
    @Override
    public InsuranceContract addContract(InsuranceContract contract) {
        return insuranceContractRepository.save(contract);
    }

    @Override
    public InsuranceContract updateContract(InsuranceContract contract) {
        return insuranceContractRepository.save(contract);
    }

    @Override
    public void deleteContract(Long id) {
        insuranceContractRepository.deleteById(id);
    }

    @Override
    public InsuranceContract getContractById(Long id) {
        return insuranceContractRepository.findById(id).get();
    }

    @Override
    public List<InsuranceContract> getAllContracts() {
        return insuranceContractRepository.findAll();
    }
}
