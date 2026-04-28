package org.example.projet_pi.Controller;

import org.example.projet_pi.Repository.AgentAssuranceRepository;
import org.example.projet_pi.Repository.AgentFinanceRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.entity.AgentAssurance;
import org.example.projet_pi.entity.AgentFinance;
import org.example.projet_pi.entity.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/clients")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClientController {

    private final ClientRepository clientRepository;
    private final AgentFinanceRepository agentFinanceRepository;
    private final AgentAssuranceRepository agentAssuranceRepository;

    public AdminClientController(ClientRepository clientRepository,
                                 AgentFinanceRepository agentFinanceRepository,
                                 AgentAssuranceRepository agentAssuranceRepository) {
        this.clientRepository = clientRepository;
        this.agentFinanceRepository = agentFinanceRepository;
        this.agentAssuranceRepository = agentAssuranceRepository;
    }

    @PutMapping("/{clientId}/assign/finance/{agentId}")
    public ResponseEntity<?> assignFinanceAgent(@PathVariable Long clientId,
                                                @PathVariable Long agentId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));

        AgentFinance agent = agentFinanceRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent Finance not found with id: " + agentId));

        client.setAgentFinance(agent);
        clientRepository.save(client);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Agent Finance assigned successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{clientId}/assign/assurance/{agentId}")
    public ResponseEntity<?> assignAssuranceAgent(@PathVariable Long clientId,
                                                  @PathVariable Long agentId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));

        AgentAssurance agent = agentAssuranceRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent Assurance not found with id: " + agentId));

        client.setAgentAssurance(agent);
        clientRepository.save(client);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Agent Assurance assigned successfully");
        return ResponseEntity.ok(response);
    }
}