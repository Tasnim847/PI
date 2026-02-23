package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.ContractStatus;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceContractRepository extends JpaRepository<InsuranceContract,Long> {
    List<InsuranceContract> findByStatus(ContractStatus status);
}
