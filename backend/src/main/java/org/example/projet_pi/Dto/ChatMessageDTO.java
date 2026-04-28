package org.example.projet_pi.Dto;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private String message;
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String sessionId;
}