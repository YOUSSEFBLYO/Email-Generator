package com.youssef.email.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("api/email")
@AllArgsConstructor
public class EmailGeneratorController {
    private final EmailService emailService;
    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestBody EmailRequest emailRequest){
        try {
            String emailReply = emailService.generateEmailReply(emailRequest);
            return ResponseEntity.ok(emailReply);
        } catch (Exception e) {
            String msg = "Error generating email: " + e.getMessage();
            return ResponseEntity.status(500).body(msg);
        }
    }
}
