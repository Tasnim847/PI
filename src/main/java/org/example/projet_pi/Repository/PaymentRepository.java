package org.example.projet_pi.Repository;

import jakarta.transaction.Transactional;
import org.example.projet_pi.entity.Payment;
import org.example.projet_pi.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByContract_ClientId(Long clientId);
    List<Payment> findByContract_AgentAssuranceId(Long agentId);
    List<Payment> findByContract_ContractId(Long contractId);


    @Query("SELECT p FROM Payment p WHERE p.contract.client.id = :clientId AND p.status = 'LATE'")
    List<Payment> findLatePaymentsByClientId(@Param("clientId") Long clientId);

    List<Payment> findAllByContract_ContractIdAndStatusOrderByPaymentDateAsc(
            Long contractId,
            PaymentStatus status
    );

    void deleteByContract_ContractId(Long contractId);
}