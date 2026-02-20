package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.entity.Client;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;

    // Injection via constructeur
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Client addClient(Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Client updateClient(Client client) {
        // Vérifier que le client existe avant update (optionnel mais recommandé)
        if (!clientRepository.existsById(client.getId())) {
            throw new RuntimeException("Client not found");
        }
        return clientRepository.save(client);
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
}
