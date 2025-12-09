package com.youssef.email.app;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.api.client.util.Value;

@Service
public class EmailService {
    
    private final WebClient webClient;
    public EmailService(WebClient webClient){
        this.webClient=webClient;
    }

    @Value("${gemini.api.url}")
     private String geminiApiurl;
    @Value("${gemini.api.key}")
        private String apiKey;
        //build the prompt
    public String generateEmailReply(EmailRequest emailrequest){
       
        String prompt=buildPrompt(emailrequest);
        //Craft a request
        Map<String,Object> requestBody=Map.of(
            "contents",new Object[]{
                Map.of("parts",new Object[]{
                    Map.of("text",prompt)})
                    
            }
            
        );
        //DO request and get response
        String response=webClient.post()
        .uri(geminiApiurl+apiKey)
        .header("Content-Type","application/json")
        .retrieve()
        .bodyToMono(String.class)
        .block();
        return extractResponseContent(response);





    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper om=new ObjectMapper();
            JsonNode root=om.readTree(response);
            return root.path("candidates")
            .get(0)
            .path("content")
            .path("parts"
            .get(0)
            .path("text")
            .asText();
            
            )
        } catch (Exception e) {
            return "Error parsing response"+e.getMessage();
        }
    
    }

    private String buildPrompt(EmailRequest emailrequest) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Generate a pro email reply for he following email content");
        if(emailrequest.getTone()!=null && !emailrequest.getTone().isEmpty()){
            prompt.append(" use a").append(emailrequest.getTone()).append(" tone.");}
            prompt.append("\n original email content:").append(emailrequest.getEmailContent());
            return prompt.toString();
    }
}



