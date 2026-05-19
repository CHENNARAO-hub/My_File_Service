package com.enterprise.fileservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterprise.fileservice.DTO.AskRequest;
import com.enterprise.fileservice.ai.AiService;
import com.enterprise.fileservice.entity.UserFile;
import com.enterprise.fileservice.repository.UserFileRepository;


@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
	

    private final UserFileRepository repository;
    private final AiService aiService;

    public AiController(
            UserFileRepository repository,
            AiService aiService) {

        this.repository = repository;
        this.aiService = aiService;
    }
    
    @PostMapping("/ask")
    public ResponseEntity<String> askQuestion(
            @RequestBody AskRequest request) {

        UUID id =
          UUID.fromString(request.getDocumentId());

        UserFile file =
            repository.findById(id)
                      .orElseThrow();

        String prompt = """
            Answer ONLY from below document.

            DOCUMENT:
            %s

            QUESTION:
            %s
            """.formatted(
                 file.getExtractedText(),
                 request.getQuestion());
        System.out.println("AI API HIT");
        System.out.println("Question: " + request.getQuestion());
        try {

            String response =
                    aiService.askQuestion(prompt);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
       
    }
}
