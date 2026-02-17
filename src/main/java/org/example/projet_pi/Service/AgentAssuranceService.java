package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.AgentAssuranceRepository;
import org.example.projet_pi.entity.AgentAssurance;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentAssuranceService implements IAgentAssuranceService {

    private final AgentAssuranceRepository agentAssuranceRepository;

    public AgentAssuranceService(AgentAssuranceRepository agentAssuranceRepository) {
        this.agentAssuranceRepository = agentAssuranceRepository;
    }

    @Override
    public AgentAssurance addAgent(AgentAssurance agentAssurance) {
        return agentAssuranceRepository.save(agentAssurance);
    }

    @Override
    public AgentAssurance updateAgent(AgentAssurance agentAssurance) {
        return agentAssuranceRepository.save(agentAssurance);
    }

    @Override
    public void deleteAgent(Long id) {
        agentAssuranceRepository.deleteById(id);
    }

    @Override
    public AgentAssurance getAgentById(Long id) {
        return agentAssuranceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent Assurance not found"));
    }

    @Override
    public List<AgentAssurance> getAllAgents() {
        return agentAssuranceRepository.findAll();
    }
}
