package com.youssef.email.app;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value; // ✅ BON IMPORT
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Avec retry automatique en cas d'erreur temporaire
     */
    public String generateEmailReply(EmailRequest emailRequest) {
        int maxRetries = 2;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return callGeminiAPI(emailRequest);
            } catch (Exception e) {
                lastException = e;
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                
                // Retry only on transient errors (connection reset, 503, timeout)
                boolean isTransient = msg.contains("connection reset") 
                    || msg.contains("503")
                    || msg.contains("unavailable")
                    || msg.contains("timeout")
                    || msg.contains("reset");
                
                if (!isTransient || attempt > maxRetries) {
                    // Non-transient error or max retries reached
                    throw e;
                }
                
                // Wait before retry (exponential backoff: 1s, 2s)
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        throw new RuntimeException("Failed after " + (maxRetries + 1) + " attempts", lastException);
    }
    
    /**
     * Appel réel à l'API Gemini (appelé avec retry)
     */
    private String callGeminiAPI(EmailRequest emailRequest) {
        String prompt = buildPrompt(emailRequest);
        
        Map<String, Object> requestBody = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", prompt)
                })
            }
        );
        
        try {
            if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("Gemini API URL or key is not configured");
            }

            String response = webClient.post()
                .uri(apiUrl + apiKey)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null) {
                throw new IllegalStateException("Empty response from Gemini API");
            }

            return extractResponseContent(response);
        } catch (WebClientResponseException wex) {
            String body = wex.getResponseBodyAsString();
            throw new RuntimeException("Gemini API error: " + wex.getStatusCode() + " - " + body, wex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extrait le texte généré depuis la réponse JSON de Gemini
     */
    private String extractResponseContent(String response) {
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }

            // If expected structure is not present, return the raw response for debugging
            return response;
            
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