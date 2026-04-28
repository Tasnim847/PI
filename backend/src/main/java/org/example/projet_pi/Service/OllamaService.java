package org.example.projet_pi.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:mistral:7b-instruct-v0.2-q4_K_M}")
    private String modelName;

    public String generateResponse(String prompt) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", modelName);
            request.put("prompt", prompt);
            request.put("stream", false);
            request.put("temperature", 0.3);
            request.put("max_tokens", 500);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    ollamaUrl + "/api/generate",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("response").asText();

        } catch (Exception e) {
            return "❌ Je rencontre des difficultés techniques. Veuillez réessayer plus tard.";
        }
    }
}