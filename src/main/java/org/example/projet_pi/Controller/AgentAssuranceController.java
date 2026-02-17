package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.IAgentAssuranceService;
import org.example.projet_pi.entity.AgentAssurance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents-assurance")
public class AgentAssuranceController {

    @Autowired
    private IAgentAssuranceService agentAssuranceService;

    // Ajouter un agent assurance
    @PostMapping("/add")
    public AgentAssurance addAgent(@RequestBody AgentAssurance agentAssurance) {
        return agentAssuranceService.addAgent(agentAssurance);
    }

    // Modifier un agent assurance
    @PutMapping("/update")
    public AgentAssurance updateAgent(@RequestBody AgentAssurance agentAssurance) {
        return agentAssuranceService.updateAgent(agentAssurance);
    }

    // Supprimer un agent assurance
    @DeleteMapping("/delete/{id}")
    public void deleteAgent(@PathVariable Long id) {
        agentAssuranceService.deleteAgent(id);
    }

    // Récupérer un agent assurance par ID
    @GetMapping("/{id}")
    public AgentAssurance getAgentById(@PathVariable Long id) {
        return agentAssuranceService.getAgentById(id);
    }

    // Récupérer tous les agents assurance
    @GetMapping("/all")
    public List<AgentAssurance> getAllAgents() {
        return agentAssuranceService.getAllAgents();
    }
}
