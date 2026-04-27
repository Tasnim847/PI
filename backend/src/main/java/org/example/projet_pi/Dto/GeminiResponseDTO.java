package org.example.projet_pi.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiResponseDTO {
    
    private List<Candidate> candidates;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}
