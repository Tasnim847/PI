package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.DocumentDTO;

import java.util.List;

public interface IDocumentService {

    DocumentDTO addDocument(DocumentDTO dto);

    DocumentDTO updateDocument(DocumentDTO dto);

    void deleteDocument(Long id);

    DocumentDTO getDocumentById(Long id);

    List<DocumentDTO> getAllDocuments();

    List<DocumentDTO> getDocumentsByClaimId(Long claimId);

    List<DocumentDTO> getDocumentsByClientId(Long clientId);
}