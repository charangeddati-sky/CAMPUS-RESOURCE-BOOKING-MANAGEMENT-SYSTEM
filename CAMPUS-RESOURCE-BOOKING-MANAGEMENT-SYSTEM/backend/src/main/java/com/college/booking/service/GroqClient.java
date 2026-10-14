package com.college.booking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GroqClient(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            @Value("${groq.model}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("your_key_here");
    }

    public String chat(String system, String user) {
        return chat(system, List.of(Map.of("role", "user", "content", user)), false);
    }

    public String chatJson(String system, String user) {
        return chat(system, List.of(Map.of("role", "user", "content", user)), true);
    }

    public String chat(String system, List<Map<String, String>> messages, boolean json) {
        if (!isConfigured()) {
            throw new IllegalStateException("GROQ_UNAVAILABLE");
        }
        try {
            return call(model, system, messages, json);
        } catch (RuntimeException primary) {
            log.warn("Groq model {} failed: {}", model, primary.getMessage());
            if (!"llama-3.1-8b-instant".equals(model)) {
                try {
                    return call("llama-3.1-8b-instant", system, messages, json);
                } catch (RuntimeException ignored) {
                    throw new IllegalStateException("GROQ_UNAVAILABLE", primary);
                }
            }
            throw new IllegalStateException("GROQ_UNAVAILABLE", primary);
        }
    }

    private String call(String useModel, String system, List<Map<String, String>> messages, boolean json) {
        List<Map<String, String>> payloadMessages = new java.util.ArrayList<>();
        payloadMessages.add(Map.of("role", "system", "content", system));
        payloadMessages.addAll(messages);
        Map<String, Object> body = new HashMap<>();
        body.put("model", useModel);
        body.put("messages", payloadMessages);
        body.put("temperature", 0.2);
        if (json) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        String raw = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("GROQ_EMPTY");
            }
            return content;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("GROQ_UNAVAILABLE", ex);
        }
    }
}
