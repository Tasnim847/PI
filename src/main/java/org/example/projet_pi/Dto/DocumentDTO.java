package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentDTO {

    private Long documentId;       // ID du document (optionnel à la création)
    private String name;           // nom du document
    private String type;           // type, ex: IMAGE, PDF
    private String filePath;       // chemin ou URL du fichier
    private LocalDateTime uploadDate; // date/heure d'upload

    private String status;

    private Long claimId;          // référence à un Claim (facultatif si le document n'est pas lié à un claim)
}