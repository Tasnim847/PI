package org.example.projet_pi.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatResponseDTO {
    private String response;
    private List<String> suggestions;
    private String timestamp;

    public ChatResponseDTO() {}

    public ChatResponseDTO(String response) {
        this.response = response;
        this.timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public ChatResponseDTO(String response, List<String> suggestions) {
        this.response = response;
        this.suggestions = suggestions;
        this.timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}