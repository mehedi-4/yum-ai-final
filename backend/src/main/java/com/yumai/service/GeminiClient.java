package com.yumai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin HTTP client for the Google Gemini generateContent API (FR-05.4, ER-01).
 * When no API key is configured, callers fall back to the statistical engine.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("${yumai.gemini.api-key}") String apiKey,
                        @Value("${yumai.gemini.model}") String model,
                        @Value("${yumai.gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** @return the model's text reply, or empty if unconfigured or the call failed. */
    public Optional<String> generate(String prompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            String response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            return text.isMissingNode() ? Optional.empty() : Optional.of(text.asText());
        } catch (Exception e) {
            log.warn("Gemini API call failed, falling back to local reply: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
