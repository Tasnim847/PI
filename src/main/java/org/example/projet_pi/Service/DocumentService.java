package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Mapper.DocumentMapper;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Repository.DocumentRepository;
import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.entity.Document;
import org.example.projet_pi.entity.DocumentStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DocumentService implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;

    @Override
    public DocumentDTO addDocument(DocumentDTO dto) {

        Claim claim = null;
        if (dto.getClaimId() != null) {
            claim = claimRepository.findById(dto.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim not found"));
        }

        Document document = DocumentMapper.toEntity(dto, claim);

        // ✅ Status par défaut
        if (document.getStatus() == null) {
            document.setStatus(DocumentStatus.UPLOADED);
        }

        document = documentRepository.save(document);
        return DocumentMapper.toDTO(document);
    }

    @Override
    public DocumentDTO updateDocument(DocumentDTO dto) {

        if (dto.getDocumentId() == null) {
            throw new IllegalArgumentException("documentId ne peut pas être null !");
        }

        Document existing = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Champs simples
        if (dto.getName() != null)
            existing.setName(dto.getName());

        if (dto.getType() != null)
            existing.setType(dto.getType());

        if (dto.getFilePath() != null)
            existing.setFilePath(dto.getFilePath());

        if (dto.getUploadDate() != null)
            existing.setUploadDate(dto.getUploadDate());

        // ✅ Modification du status
        if (dto.getStatus() != null) {
            existing.setStatus(DocumentStatus.valueOf(dto.getStatus()));
        }

        // Mise à jour du claim si fourni
        if (dto.getClaimId() != null) {
            Claim claim = claimRepository.findById(dto.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim not found"));
            existing.setClaim(claim);
        }

        existing = documentRepository.save(existing);

        return DocumentMapper.toDTO(existing);
    }

    @Override
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }

    @Override
    public DocumentDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return DocumentMapper.toDTO(document);
    }

    @Override
    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll()
                .stream()
                .map(DocumentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByClaimId(Long claimId) {
        return documentRepository.findByClaimClaimId(claimId)
                .stream()
                .map(DocumentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByClientId(Long clientId) {
        return documentRepository.findByClientId(clientId)
                .stream()
                .map(DocumentMapper::toDTO)
                .collect(Collectors.toList());
    }
}