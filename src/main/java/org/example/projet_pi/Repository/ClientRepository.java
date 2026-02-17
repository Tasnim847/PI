package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // Trouver les clients par Agent Finance
    List<Client> findByAgentFinanceId(Long agentFinanceId);

    // Trouver les clients par Agent Assurance
    List<Client> findByAgentAssuranceId(Long agentAssuranceId);


}
