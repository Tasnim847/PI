package org.example.projet_pi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key:AIzaSyDWLNrIS33YaxN1_rysdNAGybXZY01Rqo8}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String geminiApiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getGeminiApiUrl() {
        return geminiApiUrl;
    }
}