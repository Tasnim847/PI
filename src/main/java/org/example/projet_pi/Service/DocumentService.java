package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Mapper.DocumentMapper;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Repository.DocumentRepository;
import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.entity.Document;
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
        document = documentRepository.save(document);
        return DocumentMapper.toDTO(document);
    }

    @Override
    public DocumentDTO updateDocument(DocumentDTO dto) {
        Document existing = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        existing.setName(dto.getName());
        existing.setType(dto.getType());
        existing.setFilePath(dto.getFilePath());
        existing.setUploadDate(dto.getUploadDate());

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
}