package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.AgentFinanceRepository;
import org.example.projet_pi.entity.AgentFinance;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentFinanceService implements IAgentFinanceService {

    private final AgentFinanceRepository agentFinanceRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentFinanceService(AgentFinanceRepository agentFinanceRepository,PasswordEncoder passwordEncoder) {
        this.agentFinanceRepository = agentFinanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AgentFinance addAgent(AgentFinance agentFinance) {

        agentFinance.setRole(Role.AGENT_FINANCE);

        if(agentFinance.getPassword() != null &&
                !agentFinance.getPassword().isEmpty()){

            agentFinance.setPassword(
                    passwordEncoder.encode(agentFinance.getPassword())
            );
        }

        return agentFinanceRepository.save(agentFinance);
    }

    @Override
    public AgentFinance updateAgent(AgentFinance agentFinance) {

        AgentFinance existingAgent = agentFinanceRepository
                .findById(agentFinance.getId())
                .orElseThrow(() -> new RuntimeException("Agent Finance not found"));

        if(agentFinance.getFirstName()!=null && !agentFinance.getFirstName().isEmpty())
            existingAgent.setFirstName(agentFinance.getFirstName());

        if(agentFinance.getLastName()!=null && !agentFinance.getLastName().isEmpty())
            existingAgent.setLastName(agentFinance.getLastName());

        if(agentFinance.getEmail()!=null && !agentFinance.getEmail().isEmpty())
            existingAgent.setEmail(agentFinance.getEmail());

        if(agentFinance.getTelephone()!=null && !agentFinance.getTelephone().isEmpty())
            existingAgent.setTelephone(agentFinance.getTelephone());

        // Password update (crypt only if changed)
        if(agentFinance.getPassword()!=null &&
                !agentFinance.getPassword().isEmpty()){

            existingAgent.setPassword(
                    passwordEncoder.encode(agentFinance.getPassword())
            );
        }

        existingAgent.setRole(Role.AGENT_FINANCE);

        return agentFinanceRepository.save(existingAgent);
    }

    @Override
    public void deleteAgent(Long id) {
        agentFinanceRepository.deleteById(id);
    }

    @Override
    public AgentFinance getAgentById(Long id) {
        return agentFinanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
    }

    @Override
    public List<AgentFinance> getAllAgents() {
        return agentFinanceRepository.findAll();
    }

    @Override
    public void changePassword(Long agentId,
                               String oldPassword,
                               String newPassword) {

        AgentFinance agent = agentFinanceRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if(!passwordEncoder.matches(oldPassword, agent.getPassword())){
            throw new RuntimeException("Old password incorrect");
        }

        agent.setPassword(passwordEncoder.encode(newPassword));

        agentFinanceRepository.save(agent);
    }
}
