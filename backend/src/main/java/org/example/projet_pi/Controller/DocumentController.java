package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Service.IDocumentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/documents")
public class DocumentController {

    private final IDocumentService documentService;

    @PostMapping("/addDoc")
    public DocumentDTO addDocument(
            @RequestBody DocumentDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        return documentService.addDocument(dto, currentUser.getUsername());
    }

    @PutMapping("/updateDoc")
    public DocumentDTO updateDocument(
            @RequestBody DocumentDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        return documentService.updateDocument(dto, currentUser.getUsername());
    }

    @DeleteMapping("/deleteDoc/{id}")
    public void deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        documentService.deleteDocument(id, currentUser.getUsername());
    }

    @GetMapping("/getDoc/{id}")
    public DocumentDTO getDocumentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return documentService.getDocumentById(id, currentUser.getUsername());
    }

    @GetMapping("/allDoc")
    public List<DocumentDTO> getAllDocuments(
            @AuthenticationPrincipal UserDetails currentUser) {
        return documentService.getAllDocuments(currentUser.getUsername());
    }

    @GetMapping("/claim/{claimId}")
    public List<DocumentDTO> getDocumentsByClaim(
            @PathVariable Long claimId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return documentService.getDocumentsByClaimId(claimId, currentUser.getUsername());
    }
}