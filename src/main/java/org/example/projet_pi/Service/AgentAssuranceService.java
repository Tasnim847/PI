package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.AgentAssuranceRepository;
import org.example.projet_pi.entity.AgentAssurance;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class AgentAssuranceService implements IAgentAssuranceService {

    private final AgentAssuranceRepository agentAssuranceRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentAssuranceService(AgentAssuranceRepository agentAssuranceRepository, PasswordEncoder passwordEncoder) {
        this.agentAssuranceRepository = agentAssuranceRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public AgentAssurance addAgent(AgentAssurance agentAssurance) {

        agentAssurance.setRole(Role.AGENT_ASSURANCE);

        // Crypter password
        if(agentAssurance.getPassword() != null){
            agentAssurance.setPassword(
                    passwordEncoder.encode(agentAssurance.getPassword())
            );
        }

        return agentAssuranceRepository.save(agentAssurance);
    }

    @Override
    public AgentAssurance updateAgent(AgentAssurance agentAssurance) {

        AgentAssurance existingAgent = agentAssuranceRepository
                .findById(agentAssurance.getId())
                .orElseThrow(() -> new RuntimeException("Agent Assurance not found"));

        // Update informations User
        if(agentAssurance.getFirstName() != null && !agentAssurance.getFirstName().isEmpty())
            existingAgent.setFirstName(agentAssurance.getFirstName());

        if(agentAssurance.getLastName() != null && !agentAssurance.getLastName().isEmpty())
            existingAgent.setLastName(agentAssurance.getLastName());

        if(agentAssurance.getEmail() != null && !agentAssurance.getEmail().isEmpty())
            existingAgent.setEmail(agentAssurance.getEmail());

        if(agentAssurance.getTelephone() != null && !agentAssurance.getTelephone().isEmpty())
            existingAgent.setTelephone(agentAssurance.getTelephone());

        // ✅ Password change (recrypt if modified)
        if(agentAssurance.getPassword() != null &&
                !agentAssurance.getPassword().isEmpty()) {

            existingAgent.setPassword(
                    passwordEncoder.encode(agentAssurance.getPassword())
            );
        }

        // Role reste fixe (sécurité)
        existingAgent.setRole(Role.AGENT_ASSURANCE);

        return agentAssuranceRepository.save(existingAgent);
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

    @Override
    public void changePassword(Long agentId,
                               String oldPassword,
                               String newPassword) {

        AgentAssurance agent = agentAssuranceRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        // Vérifier ancien password
        if(!passwordEncoder.matches(oldPassword, agent.getPassword())){
            throw new RuntimeException("Old password incorrect");
        }

        // Encoder nouveau password
        agent.setPassword(passwordEncoder.encode(newPassword));

        agentAssuranceRepository.save(agent);
    }
}
