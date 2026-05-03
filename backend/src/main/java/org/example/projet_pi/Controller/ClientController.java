
package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Dto.ChangePasswordRequest;
import org.example.projet_pi.Dto.ClientWithAgentsDTO;
import org.example.projet_pi.Service.IClientService;
import org.example.projet_pi.entity.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientService clientService;

    // =============================
    // ADMIN add client (multipart/form-data)
    // =============================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public Client addClient(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("telephone") String telephone,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        Client client = new Client();
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setEmail(email);
        client.setPassword(password);
        client.setTelephone(telephone);

        return clientService.addClient(client, photo);
    }

    // =============================
    // ADMIN + CLIENT update client info (multipart/form-data)
    // =============================
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT') and (#id == authentication.principal.id or hasRole('ADMIN'))")
    @PutMapping(value = "/update/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateClient(
            @PathVariable Long id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telephone,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        Client client = new Client();
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setEmail(email);
        client.setTelephone(telephone);

        Client updatedClient = clientService.updateClientById(id, client, photo);
        return ResponseEntity.ok(updatedClient);
    }

    // =============================
    // CLIENT change password ONLY
    // =============================
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request){
        clientService.changePassword(
                request.getId(),
                request.getOldPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok("Password changed successfully");
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
    // Ajouter UNIQUEMENT cette méthode dans ClientController.java
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-with-agents")
    public ResponseEntity<List<ClientWithAgentsDTO>> getAllClientsWithAgents() {
        return ResponseEntity.ok(clientService.getAllClientsWithAgents());
    }



}