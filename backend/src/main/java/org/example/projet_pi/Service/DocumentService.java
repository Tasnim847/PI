package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Mapper.DocumentMapper;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Repository.DocumentRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.entity.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DocumentService implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    @Override
    public DocumentDTO addDocument(DocumentDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof Client)) {
            throw new AccessDeniedException("Seuls les clients peuvent ajouter des documents");
        }

        Client client = (Client) user;

        Claim claim = null;
        if (dto.getClaimId() != null) {
            claim = claimRepository.findById(dto.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim not found"));

            // Vérifier que le claim appartient bien au client
            if (!claim.getClient().getId().equals(client.getId())) {
                throw new AccessDeniedException("Ce claim ne vous appartient pas");
            }
        }

        Document document = DocumentMapper.toEntity(dto, claim);
        document.setClient(client); // Lier le document au client

        if (document.getStatus() == null) {
            document.setStatus(DocumentStatus.UPLOADED);
        }

        document = documentRepository.save(document);
        return DocumentMapper.toDTO(document);
    }

    @Override
    public DocumentDTO updateDocument(DocumentDTO dto, String userEmail) {
        if (dto.getDocumentId() == null) {
            throw new IllegalArgumentException("documentId ne peut pas être null !");
        }

        Document existing = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que le document appartient au client
        if (user instanceof Client) {
            if (!existing.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez modifier que vos propres documents");
            }
        }

        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getFilePath() != null) existing.setFilePath(dto.getFilePath());
        if (dto.getUploadDate() != null) existing.setUploadDate(dto.getUploadDate());
        if (dto.getStatus() != null) existing.setStatus(DocumentStatus.valueOf(dto.getStatus()));

        // Mise à jour du claim si fourni
        if (dto.getClaimId() != null) {
            Claim claim = claimRepository.findById(dto.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim not found"));

            // Vérifier que le claim appartient au client
            if (user instanceof Client && !claim.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ce claim ne vous appartient pas");
            }
            existing.setClaim(claim);
        }

        existing = documentRepository.save(existing);
        return DocumentMapper.toDTO(existing);
    }

    @Override
    public void deleteDocument(Long id, String userEmail) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client) {
            if (!document.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres documents");
            }
        }

        documentRepository.deleteById(id);
    }

    @Override
    public DocumentDTO getDocumentById(Long id, String userEmail) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits
        if (user instanceof Client) {
            if (!document.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez consulter que vos propres documents");
            }
        } else if (user instanceof AgentAssurance) {
            AgentAssurance agent = (AgentAssurance) user;
            if (!document.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce document n'appartient pas à un de vos clients");
            }
        }

        return DocumentMapper.toDTO(document);
    }

    @Override
    public List<DocumentDTO> getAllDocuments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client) {
            // Client: ses propres documents
            return documentRepository.findByClientId(user.getId())
                    .stream()
                    .map(DocumentMapper::toDTO)
                    .collect(Collectors.toList());
        } else if (user instanceof AgentAssurance) {
            // Agent: documents de ses clients
            AgentAssurance agent = (AgentAssurance) user;
            return documentRepository.findAll().stream()
                    .filter(doc -> doc.getClient().getAgentAssurance() != null
                            && doc.getClient().getAgentAssurance().getId().equals(agent.getId()))
                    .map(DocumentMapper::toDTO)
                    .collect(Collectors.toList());
        }
        // Admin: tous les documents
        return documentRepository.findAll()
                .stream()
                .map(DocumentMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentDTO> getDocumentsByClaimId(Long claimId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        // Vérification des droits
        if (user instanceof Client) {
            if (!claim.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ce claim ne vous appartient pas");
            }
        } else if (user instanceof AgentAssurance) {
            AgentAssurance agent = (AgentAssurance) user;
            if (!claim.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce claim n'appartient pas à un de vos clients");
            }
        }

        return documentRepository.findByClaimClaimId(claimId)
                .stream()
                .map(DocumentMapper::toDTO)
                .collect(Collectors.toList());
    }
}