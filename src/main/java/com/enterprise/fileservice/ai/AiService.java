package com.enterprise.fileservice.ai;

import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

@Service
public class AiService {

    private final Client client;

    public AiService() {

        this.client = Client.builder()
                .apiKey("AIzaSyBNYgdyNj_L9cv8_ans1YTGLvRbEchciZE")
                .build();
    }
   

    public String askQuestion(String prompt) {

        try {

            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.0-flash",
                            prompt,
                            null
                    );

            return response.text();

        } catch (Exception e) {

            e.printStackTrace();

            return "AI Error : " + e.getMessage();
        }
    }
}