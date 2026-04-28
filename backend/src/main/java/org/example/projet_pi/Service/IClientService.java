
package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ClientWithAgentsDTO;
import org.example.projet_pi.entity.Client;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IClientService {

    Client addClient(Client client, MultipartFile photo);

    Client updateClientById(Long id, Client clientRequest, MultipartFile photo);

    void deleteClient(Long id);

    Client getClientById(Long id);

    List<Client> getAllClients();

    List<Client> getClientsByAgentFinance(Long agentFinanceId);

    List<Client> getClientsByAgentAssurance(Long agentAssuranceId);

    void changePassword(Long clientId, String oldPassword, String newPassword);

    List<ClientWithAgentsDTO> getAllClientsWithAgents();
}