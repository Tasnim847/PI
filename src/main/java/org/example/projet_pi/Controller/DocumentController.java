package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Service.IDocumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/documents")
public class DocumentController {

    private final IDocumentService documentService;

    @PostMapping("/addDoc")
    public DocumentDTO addDocument(@RequestBody DocumentDTO dto) {
        return documentService.addDocument(dto);
    }

    @PutMapping("/updateDoc")
    public DocumentDTO updateDocument(@RequestBody DocumentDTO dto) {
        return documentService.updateDocument(dto);
    }

    @DeleteMapping("/deleteDoc/{id}")
    public void deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
    }

    @GetMapping("/getDoc/{id}")
    public DocumentDTO getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id);
    }

    @GetMapping("/allDoc")
    public List<DocumentDTO> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @GetMapping("/claim/{claimId}")
    public List<DocumentDTO> getDocumentsByClaim(@PathVariable Long claimId) {
        return documentService.getDocumentsByClaimId(claimId);
    }

    @GetMapping("/client/{clientId}")
    public List<DocumentDTO> getDocumentsByClient(@PathVariable Long clientId) {
        return documentService.getDocumentsByClientId(clientId);
    }
}