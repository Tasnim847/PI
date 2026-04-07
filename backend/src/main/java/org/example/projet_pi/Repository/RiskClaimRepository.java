package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.RiskClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskClaimRepository extends JpaRepository<RiskClaim, Long> {
}