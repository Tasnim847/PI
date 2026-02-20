package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Client;

import java.util.List;

public interface IClientService {

    // Ajouter un client
    Client addClient(Client client);

    // Modifier un client
    Client updateClient(Client client);

    // Supprimer un client
    void deleteClient(Long id);

    // Récupérer un client par ID
    Client getClientById(Long id);

    // Récupérer tous les clients
    List<Client> getAllClients();

    // Récupérer les clients d’un Agent Finance
    List<Client> getClientsByAgentFinance(Long agentFinanceId);

    // Récupérer les clients d’un Agent Assurance
    List<Client> getClientsByAgentAssurance(Long agentAssuranceId);
}
