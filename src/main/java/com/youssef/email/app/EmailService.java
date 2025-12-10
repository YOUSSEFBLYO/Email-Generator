package com.youssef.email.app;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value; // ✅ BON IMPORT
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailService {
    
    private final WebClient webClient;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    // Constructor
    public EmailService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * Génère une réponse email professionnelle en utilisant l'API Gemini
     */
    public String generateEmailReply(EmailRequest emailRequest) {
        // 1. Construire le prompt
        String prompt = buildPrompt(emailRequest);
        
        // 2. Créer le corps de la requête pour Gemini API
        Map<String, Object> requestBody = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", prompt)
                })
            }
        );
        
        // 3. Envoyer la requête et récupérer la réponse
        String response = webClient.post()
            .uri(apiUrl + apiKey)
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        // 4. Extraire le contenu de la réponse
        return extractResponseContent(response);
    }
    
    /**
     * Extrait le texte généré depuis la réponse JSON de Gemini
     */
    private String extractResponseContent(String response) {
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(response);
            
            return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();
            
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }
    
    /**
     * Construit le prompt pour l'API Gemini
     */
    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content.");
        
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append(" Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        
        prompt.append("\n\nOriginal email content:\n").append(emailRequest.getEmailContent());
        
        return prompt.toString();
    }
}