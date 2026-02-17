package org.example.projet_pi.Service;

import org.example.projet_pi.entity.AgentFinance;

import java.util.List;

public interface IAgentFinanceService {

    AgentFinance addAgent(AgentFinance agentFinance);

    AgentFinance updateAgent(AgentFinance agentFinance);

    void deleteAgent(Long id);

    AgentFinance getAgentById(Long id);

    List<AgentFinance> getAllAgents();
}
