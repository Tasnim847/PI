package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.DocumentDTO;
import java.util.List;

public interface IDocumentService {

    DocumentDTO addDocument(DocumentDTO dto, String userEmail);

    DocumentDTO updateDocument(DocumentDTO dto, String userEmail);

    void deleteDocument(Long id, String userEmail);

    DocumentDTO getDocumentById(Long id, String userEmail);

    List<DocumentDTO> getAllDocuments(String userEmail);

    List<DocumentDTO> getDocumentsByClaimId(Long claimId, String userEmail);
}