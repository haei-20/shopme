package com.example.gearshop.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/chatbot")
public class ChatBotController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatBotController.class);

    @Value("${OLLAMA_BASE_URL:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${OLLAMA_MODEL:qwen2.5:7b}")
    private String ollamaModel;

    @PostMapping("/sendMessage")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> input) {
        if (!input.containsKey("message") || input.get("message").trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        String userMessage = input.get("message").trim();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("model", ollamaModel);
        requestData.put("prompt", userMessage);
        requestData.put("stream", false);

        RestTemplate restTemplate = new RestTemplate();
        try {
            String ollamaApiUrl = ollamaBaseUrl + "/api/generate";
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaApiUrl, requestData, Map.class);
            if (response.getStatusCode().value() != 200) {
                return ResponseEntity.status(response.getStatusCode().value()).body(Map.of("error", "Ollama API error"));
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("response")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unexpected API response format"));
            }

            String aiResponse = responseBody.get("response").toString().trim();

            return ResponseEntity.ok(Map.of("response", aiResponse));
        } catch (RestClientException e) {
            LOGGER.error("Failed to call Ollama API", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch response from Ollama"));
        }
    }
}
