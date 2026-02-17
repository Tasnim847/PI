package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.IAgentFinanceService;
import org.example.projet_pi.entity.AgentFinance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents/finance")
public class AgentFinanceController {

    @Autowired
    private IAgentFinanceService agentFinanceService;

    // Ajouter un agent finance
    @PostMapping("/add")
    public AgentFinance addAgent(@RequestBody AgentFinance agentFinance) {
        return agentFinanceService.addAgent(agentFinance);
    }

    // Modifier un agent finance
    @PutMapping("/update")
    public AgentFinance updateAgent(@RequestBody AgentFinance agentFinance) {
        return agentFinanceService.updateAgent(agentFinance);
    }

    // Supprimer un agent finance
    @DeleteMapping("/delete/{id}")
    public void deleteAgent(@PathVariable Long id) {
        agentFinanceService.deleteAgent(id);
    }

    // Récupérer un agent finance par ID
    @GetMapping("/{id}")
    public AgentFinance getAgentById(@PathVariable Long id) {
        return agentFinanceService.getAgentById(id);
    }

    // Récupérer tous les agents finance
    @GetMapping("/all")
    public List<AgentFinance> getAllAgents() {
        return agentFinanceService.getAllAgents();
    }

    // Récupérer tous les clients d’un agent finance
    @GetMapping("/{id}/clients")
    public List<?> getClientsByAgent(@PathVariable Long id) {
        AgentFinance agent = agentFinanceService.getAgentById(id);
        return agent.getClients(); // Utilise @JsonManagedReference côté AgentFinance
    }
}
