package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByClientId(Long clientId);
    List<Claim> findByContract_AgentAssuranceId(Long agentId);

    boolean existsByContract_ContractIdAndClient_IdAndStatusIn(
            Long contractId,
            Long clientId,
            List<ClaimStatus> statuses
    );
}
