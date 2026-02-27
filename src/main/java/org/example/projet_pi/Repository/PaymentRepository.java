package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByContract_ClientId(Long clientId);
    List<Payment> findByContract_AgentAssuranceId(Long agentId);
    List<Payment> findByContract_ContractId(Long contractId);
}