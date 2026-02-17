package org.example.projet_pi.Service;

import org.example.projet_pi.entity.AgentAssurance;

import java.util.List;

public interface IAgentAssuranceService {

    AgentAssurance addAgent(AgentAssurance agentAssurance);

    AgentAssurance updateAgent(AgentAssurance agentAssurance);

    void deleteAgent(Long id);

    AgentAssurance getAgentById(Long id);

    List<AgentAssurance> getAllAgents();
}
