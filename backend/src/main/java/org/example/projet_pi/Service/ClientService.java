package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Dto.ClientWithAgentsDTO;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Client addClient(Client client, MultipartFile photo) {
        client.setRole(Role.CLIENT);

        if (client.getPassword() != null && !client.getPassword().isEmpty()) {
            client.setPassword(passwordEncoder.encode(client.getPassword()));
        }

        if (photo != null && !photo.isEmpty()) {
            String fileName = uploadPhoto(photo);
            client.setPhoto(fileName);
        }

        return clientRepository.save(client);
    }

    @Override
    public Client updateClientById(Long id, Client clientRequest, MultipartFile photo) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (clientRequest.getFirstName() != null)
            existingClient.setFirstName(clientRequest.getFirstName());
        if (clientRequest.getLastName() != null)
            existingClient.setLastName(clientRequest.getLastName());
        if (clientRequest.getEmail() != null)
            existingClient.setEmail(clientRequest.getEmail());
        if (clientRequest.getTelephone() != null)
            existingClient.setTelephone(clientRequest.getTelephone());

        if (clientRequest.getPassword() != null) {
            throw new RuntimeException("Password update not allowed here");
        }

        if (photo != null && !photo.isEmpty()) {
            String fileName = uploadPhoto(photo);
            existingClient.setPhoto(fileName);
        }

        return clientRepository.save(existingClient);
    }

    @Override
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Client not found");
        }
        clientRepository.deleteById(id);
    }

    @Override
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    @Override
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public List<Client> getClientsByAgentFinance(Long agentFinanceId) {
        return clientRepository.findByAgentFinanceId(agentFinanceId);
    }

    @Override
    public List<Client> getClientsByAgentAssurance(Long agentAssuranceId) {
        return clientRepository.findByAgentAssuranceId(agentAssuranceId);
    }

    @Override
    public void changePassword(Long clientId, String oldPassword, String newPassword) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!passwordEncoder.matches(oldPassword, client.getPassword())) {
            throw new RuntimeException("Old password incorrect");
        }

        client.setPassword(passwordEncoder.encode(newPassword));
        clientRepository.save(client);
    }

    private String uploadPhoto(MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Erreur upload photo");
        }
    }
    public List<ClientWithAgentsDTO> getAllClientsWithAgents() {
        List<Client> clients = clientRepository.findAll();

        return clients.stream().map(client -> {
            ClientWithAgentsDTO dto = new ClientWithAgentsDTO();
            dto.setId(client.getId());
            dto.setFirstName(client.getFirstName());
            dto.setLastName(client.getLastName());
            dto.setEmail(client.getEmail());
            dto.setTelephone(client.getTelephone());
            dto.setRole(client.getRole().name());
            dto.setPhoto(client.getPhoto());
            dto.setAnnualIncome(client.getAnnualIncome());
            dto.setCreatedAt(client.getCreatedAt());

            // 🔥 Ajouter l'agent finance (ID et nom)
            if (client.getAgentFinance() != null) {
                dto.setAgentFinanceId(client.getAgentFinance().getId());
                dto.setAgentFinanceName(client.getAgentFinance().getFirstName() + " " + client.getAgentFinance().getLastName());
            } else {
                dto.setAgentFinanceId(null);
                dto.setAgentFinanceName("Not assigned");
            }

            // 🔥 Ajouter l'agent assurance
            if (client.getAgentAssurance() != null) {
                dto.setAgentAssuranceId(client.getAgentAssurance().getId());
                dto.setAgentAssuranceName(client.getAgentAssurance().getFirstName() + " " + client.getAgentAssurance().getLastName());
            } else {
                dto.setAgentAssuranceId(null);
                dto.setAgentAssuranceName("Not assigned");
            }

            return dto;
        }).collect(Collectors.toList());
    }
}