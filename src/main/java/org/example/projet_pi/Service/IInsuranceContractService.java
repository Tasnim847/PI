package org.example.projet_pi.Service;

import org.example.projet_pi.entity.InsuranceContract;

import java.util.List;

public interface IInsuranceContractService {

    //Iservice

    public InsuranceContract addContract(InsuranceContract contract);

    public InsuranceContract updateContract(InsuranceContract contract);

    public void deleteContract(Long id);

    public InsuranceContract getContractById(Long id);

    public List<InsuranceContract> getAllContracts();

}
