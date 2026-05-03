
package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.AgentFinanceRepository;
import org.example.projet_pi.entity.AgentFinance;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentFinanceService implements IAgentFinanceService {

    private final AgentFinanceRepository agentFinanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AgentFinance addAgent(AgentFinance agentFinance, MultipartFile photo) {
        agentFinance.setRole(Role.AGENT_FINANCE);

        if(agentFinance.getPassword() != null && !agentFinance.getPassword().isEmpty()){
            agentFinance.setPassword(passwordEncoder.encode(agentFinance.getPassword()));
        }

        if(photo != null && !photo.isEmpty()){
            String fileName = uploadPhoto(photo);
            agentFinance.setPhoto(fileName);
        }

        return agentFinanceRepository.save(agentFinance);
    }

    @Override
    public AgentFinance updateAgentById(Long id, AgentFinance agentFinance, MultipartFile photo) {
        AgentFinance existingAgent = agentFinanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent Finance not found"));

        if(agentFinance.getFirstName() != null && !agentFinance.getFirstName().isEmpty())
            existingAgent.setFirstName(agentFinance.getFirstName());

        if(agentFinance.getLastName() != null && !agentFinance.getLastName().isEmpty())
            existingAgent.setLastName(agentFinance.getLastName());

        if(agentFinance.getEmail() != null && !agentFinance.getEmail().isEmpty())
            existingAgent.setEmail(agentFinance.getEmail());

        if(agentFinance.getTelephone() != null && !agentFinance.getTelephone().isEmpty())
            existingAgent.setTelephone(agentFinance.getTelephone());

        if(agentFinance.getPassword() != null && !agentFinance.getPassword().isEmpty())
            existingAgent.setPassword(passwordEncoder.encode(agentFinance.getPassword()));

        if(photo != null && !photo.isEmpty()){
            String fileName = uploadPhoto(photo);
            existingAgent.setPhoto(fileName);
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
                .orElseThrow(() -> new RuntimeException("Agent Finance not found"));
    }

    @Override
    public List<AgentFinance> getAllAgents() {
        return agentFinanceRepository.findAll();
    }

    @Override
    public void changePassword(Long agentId, String oldPassword, String newPassword) {
        AgentFinance agent = agentFinanceRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if(!passwordEncoder.matches(oldPassword, agent.getPassword())){
            throw new RuntimeException("Old password incorrect");
        }

        agent.setPassword(passwordEncoder.encode(newPassword));
        agentFinanceRepository.save(agent);
    }

    // Dans ClientService.java, AgentFinanceService.java, AgentAssuranceService.java

    private String uploadPhoto(MultipartFile file) {
        try {
            // Chemin absolu vers le dossier uploads dans le projet
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + "/uploads/";

            // Créer un nom de fichier unique
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            // Créer le dossier s'il n'existe pas
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            // Sauvegarder le fichier
            Path filePath = path.resolve(fileName);
            Files.write(filePath, file.getBytes());

            System.out.println("Photo saved to: " + filePath.toString());

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur upload photo: " + e.getMessage());
        }
    }
}