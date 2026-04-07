package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.AgentFinance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentFinanceRepository extends JpaRepository<AgentFinance, Long> {
}
