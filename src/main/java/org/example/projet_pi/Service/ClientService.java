package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository,
                         PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===============================
    // ✅ Ajouter Client (Password crypté)
    // ===============================
    @Override
    public Client addClient(Client client) {

        client.setRole(Role.CLIENT);

        // 🔐 Crypter password
        if (client.getPassword() != null && !client.getPassword().isEmpty()) {
            client.setPassword(passwordEncoder.encode(client.getPassword()));
        }

        return clientRepository.save(client);
    }

    @Override
    public Client updateClientInfo(Long id, Client clientRequest){

        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if(clientRequest.getFirstName()!=null)
            existingClient.setFirstName(clientRequest.getFirstName());

        if(clientRequest.getLastName()!=null)
            existingClient.setLastName(clientRequest.getLastName());

        if(clientRequest.getEmail()!=null)
            existingClient.setEmail(clientRequest.getEmail());

        if(clientRequest.getTelephone()!=null)
            existingClient.setTelephone(clientRequest.getTelephone());
        if(clientRequest.getPassword()!=null){
            throw new RuntimeException("Password update not allowed here");
        }

        return clientRepository.save(existingClient);

    }

    // ===============================
    // ✅ Update Client
    // ===============================
    /*
    @Override
    public Client updateClientInfo(Long id, Client clientRequest){

        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if(clientRequest.getFirstName()!=null)
            existingClient.setFirstName(clientRequest.getFirstName());

        if(clientRequest.getLastName()!=null)
            existingClient.setLastName(clientRequest.getLastName());

        if(clientRequest.getEmail()!=null)
            existingClient.setEmail(clientRequest.getEmail());

        if(clientRequest.getTelephone()!=null)
            existingClient.setTelephone(clientRequest.getTelephone());
        if(clientRequest.getPassword()!=null){
            throw new RuntimeException("Password update not allowed here");
        }

        return clientRepository.save(existingClient);
    }

     */
    // ===============================
    // ✅ Delete Client
    // ===============================
    @Override
    public void deleteClient(Long id) {

        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Client not found");
        }

        clientRepository.deleteById(id);
    }

    // ===============================
    // ✅ Get Client By ID
    // ===============================
    @Override
    public Client getClientById(Long id) {

        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    // ===============================
    // ✅ Get All Clients
    // ===============================
    @Override
    public List<Client> getAllClients() {

        return clientRepository.findAll();
    }

    // ===============================
    // ✅ Clients par Agent Finance
    // ===============================
    @Override
    public List<Client> getClientsByAgentFinance(Long agentFinanceId) {

        return clientRepository.findByAgentFinanceId(agentFinanceId);
    }

    // ===============================
    // ✅ Clients par Agent Assurance
    // ===============================
    @Override
    public List<Client> getClientsByAgentAssurance(Long agentAssuranceId) {

        return clientRepository.findByAgentAssuranceId(agentAssuranceId);
    }

    @Override
    public void changePassword(Long clientId,
                               String oldPassword,
                               String newPassword) {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Vérifier ancien password
        if(!passwordEncoder.matches(oldPassword, client.getPassword())){
            throw new RuntimeException("Old password incorrect");
        }

        // Encoder nouveau password
        client.setPassword(passwordEncoder.encode(newPassword));

        clientRepository.save(client);
    }
}