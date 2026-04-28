package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ClientAccountDTO;
import org.example.projet_pi.Service.ClientAccountService;
import org.example.projet_pi.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client")
public class ClientAccountController {

    private final ClientAccountService clientAccountService;

    public ClientAccountController(ClientAccountService clientAccountService) {
        this.clientAccountService = clientAccountService;
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ClientAccountDTO>> getMyAccounts(Authentication auth) {
        Long clientId = getCurrentClientId(auth);
        return ResponseEntity.ok(clientAccountService.getClientAccounts(clientId));
    }

    @GetMapping("/accounts/{accountId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getAccountById(@PathVariable Long accountId,
                                            Authentication auth) {
        Long clientId = getCurrentClientId(auth);
        return ResponseEntity.ok(clientAccountService.getClientAccountById(accountId, clientId));
    }

    @GetMapping("/accounts/by-rip/{rip}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getAccountByRip(@PathVariable String rip,
                                             Authentication auth) {
        Long clientId = getCurrentClientId(auth);
        return ResponseEntity.ok(clientAccountService.getAccountByRip(rip, clientId));
    }

    // ✅ CORRECTION : Utiliser CustomUserPrincipal
    private Long getCurrentClientId(Authentication auth) {
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        return principal.getId();
    }
}