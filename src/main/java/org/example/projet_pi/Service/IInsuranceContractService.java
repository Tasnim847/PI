package org.example.projet_pi.Service;


import org.example.projet_pi.Dto.InsuranceContractDTO;

import java.util.List;

public interface IInsuranceContractService {

    InsuranceContractDTO addContract(InsuranceContractDTO dto);

    InsuranceContractDTO updateContract(InsuranceContractDTO dto);

    void deleteContract(Long id);

    InsuranceContractDTO getContractById(Long id);

    List<InsuranceContractDTO> getAllContracts();
}
