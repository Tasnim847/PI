package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.AgentAssurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentAssuranceRepository extends JpaRepository<AgentAssurance, Long> {
}
