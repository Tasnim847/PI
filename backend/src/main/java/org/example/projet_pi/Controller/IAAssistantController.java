package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ChatMessageDTO;
import org.example.projet_pi.Dto.ChatResponseDTO;
import org.example.projet_pi.Service.IntelligentBankingAssistant;
import org.example.projet_pi.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ia-assistant")
public class IAAssistantController {

    private final IntelligentBankingAssistant assistant;

    public IAAssistantController(IntelligentBankingAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatMessageDTO message,
                                                Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        message.setUserId(principal.getId());
        message.setUserFirstName(principal.getFirstName());  // ← AJOUTER
        message.setUserLastName(principal.getLastName());    // ← AJOUTER

        if (message.getSessionId() == null) {
            message.setSessionId(UUID.randomUUID().toString());
        }

        ChatResponseDTO response = assistant.processMessage(message);
        return ResponseEntity.ok(response);
    }
}