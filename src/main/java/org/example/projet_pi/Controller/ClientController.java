package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.IClientService;
import org.example.projet_pi.entity.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private IClientService clientService;

    // Ajouter un client
    @PostMapping("/add")
    public Client addClient(@RequestBody Client client) {
        return clientService.addClient(client);
    }

    // Modifier un client
    @PutMapping("/update")
    public Client updateClient(@RequestBody Client client) {
        return clientService.updateClient(client);
    }

    // Supprimer un client
    @DeleteMapping("/delete/{id}")
    public void deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
    }

    // Récupérer un client par ID
    @GetMapping("/{id}")
    public Client getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

    // Récupérer tous les clients
    @GetMapping("/all")
    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    // Récupérer les clients d’un Agent Finance
    @GetMapping("/finance/{agentId}")
    public List<Client> getClientsByAgentFinance(@PathVariable Long agentId) {
        return clientService.getClientsByAgentFinance(agentId);
    }

    // Récupérer les clients d’un Agent Assurance
    @GetMapping("/assurance/{agentId}")
    public List<Client> getClientsByAgentAssurance(@PathVariable Long agentId) {
        return clientService.getClientsByAgentAssurance(agentId);
    }
}
