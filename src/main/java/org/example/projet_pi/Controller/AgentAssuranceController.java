package org.example.projet_pi.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Dto.ChangePasswordRequest;
import org.example.projet_pi.Service.IAgentAssuranceService;
import org.example.projet_pi.entity.AgentAssurance;
import org.example.projet_pi.entity.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents-assurance")
@RequiredArgsConstructor
public class AgentAssuranceController {

    private final IAgentAssuranceService agentAssuranceService;

    //  ADMIN seulement peut ajouter un agent
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public AgentAssurance addAgent(@Valid  @RequestBody AgentAssurance agentAssurance) {
        agentAssurance.setRole(Role.AGENT_ASSURANCE);
        return agentAssuranceService.addAgent(agentAssurance);
    }

    //  ADMIN seulement peut modifier agent
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public AgentAssurance updateAgent(@Valid @RequestBody AgentAssurance agentAssurance) {
        agentAssurance.setRole(Role.AGENT_ASSURANCE);
        return agentAssuranceService.updateAgent(agentAssurance);
    }

    //  ADMIN seulement peut supprimer agent
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteAgent(@PathVariable Long id) {
        agentAssuranceService.deleteAgent(id);
    }

    //  ADMIN + AGENT peuvent voir un agent
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_ASSURANCE')")
    @GetMapping("/{id}")
    public AgentAssurance getAgentById(@PathVariable Long id) {
        return agentAssuranceService.getAgentById(id);
    }

    //  ADMIN seulement voir tous les agents
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<AgentAssurance> getAllAgents() {
        return agentAssuranceService.getAllAgents();
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_ASSURANCE')")
    public ResponseEntity<?> changePassword(
            @Valid   @RequestBody ChangePasswordRequest request){

        agentAssuranceService.changePassword(
                request.getId(),
                request.getOldPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password changed successfully");
    }


}