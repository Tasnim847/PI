package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Dto.ChangePasswordRequest;
import org.example.projet_pi.Service.IAgentFinanceService;
import org.example.projet_pi.entity.AgentFinance;
import org.example.projet_pi.entity.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents/finance")
@RequiredArgsConstructor
public class AgentFinanceController {

    private final IAgentFinanceService agentFinanceService;

    //  ADMIN seulement peut ajouter agent finance
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public AgentFinance addAgent(@RequestBody AgentFinance agentFinance) {

        agentFinance.setRole(Role.AGENT_FINANCE);
        return agentFinanceService.addAgent(agentFinance);
    }

    //  ADMIN seulement peut modifier agent finance
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public AgentFinance updateAgent(@RequestBody AgentFinance agentFinance) {

        agentFinance.setRole(Role.AGENT_FINANCE);
        return agentFinanceService.updateAgent(agentFinance);
    }

    //  ADMIN seulement peut supprimer
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteAgent(@PathVariable Long id) {
        agentFinanceService.deleteAgent(id);
    }

    //  ADMIN + AGENT_FINANCE peuvent voir un agent
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE')")
    @GetMapping("/{id}")
    public AgentFinance getAgentById(@PathVariable Long id) {
        return agentFinanceService.getAgentById(id);
    }

    // ADMIN seulement voir tous les agents finance
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<AgentFinance> getAllAgents() {
        return agentFinanceService.getAllAgents();
    }

    //  Agent finance voit ses clients seulement
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE')")
    @GetMapping("/{id}/clients")
    public List<?> getClientsByAgent(@PathVariable Long id) {

        AgentFinance agent = agentFinanceService.getAgentById(id);
        return agent.getClients();
    }
    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE')")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request){

        agentFinanceService.changePassword(
                request.getId(),
                request.getOldPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password changed successfully");
    }
}