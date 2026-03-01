package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Dto.ChangePasswordRequest;
import org.example.projet_pi.Service.IClientService;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientService clientService;

    // =============================
    // ADMIN + CLIENT update info
    // =============================
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT') and (#id == authentication.principal.id or hasRole('ADMIN'))")
    public Client updateClient(
            @PathVariable Long id,
            @RequestBody Client client){
        return clientService.updateClientInfo(id, client);
    }

    // =============================
    // CLIENT change password ONLY
    // =============================
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request){

        clientService.changePassword(
                request.getId(),
                request.getOldPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password changed successfully");
    }

    // =============================
    // ADMIN add client
    // =============================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Client addClient(@RequestBody Client client){
        return clientService.addClient(client);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteClient(@PathVariable Long id){
        clientService.deleteClient(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT_ASSURANCE','AGENT_FINANCE')")
    @GetMapping("/{id}")
    public Client getClientById(@PathVariable Long id){
        return clientService.getClientById(id);
    }
}