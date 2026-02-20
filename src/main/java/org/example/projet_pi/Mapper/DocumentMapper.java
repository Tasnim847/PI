package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.entity.Document;

public class DocumentMapper {

    // Entity -> DTO
    public static DocumentDTO toDTO(Document document) {
        if (document == null) return null;

        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentId(document.getDocumentId());
        dto.setName(document.getName());
        dto.setType(document.getType());
        dto.setFilePath(document.getFilePath());
        dto.setUploadDate(document.getUploadDate());

        if (document.getClaim() != null)
            dto.setClaimId(document.getClaim().getClaimId());

        return dto;
    }

    // DTO -> Entity
    public static Document toEntity(DocumentDTO dto, Claim claim) {
        if (dto == null) return null;

        Document document = new Document();
        document.setDocumentId(dto.getDocumentId());
        document.setName(dto.getName());
        document.setType(dto.getType());
        document.setFilePath(dto.getFilePath());
        document.setUploadDate(dto.getUploadDate());

        document.setClaim(claim);

        return document;
    }
}