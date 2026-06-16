package com.graduation.mathai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.mathai.config.AiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level AI chat client — handles HTTP communication with LLM API.
 * Extracted from AiService for separation of concerns.
 */
@Component
public class AiChatClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiChatClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Send a chat request and return the message content.
     *
     * @param prompt     the user prompt
     * @param jsonObject whether to request JSON structured output
     * @return the AI response text content
     */
    public String chat(String prompt, boolean jsonObject) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "Return exactly what the user asks for. Keep output concise and structured."),
                Map.of("role", "user", "content", prompt)
        ));
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 2048);
        payload.put("stream", false);
        if (jsonObject) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        HttpResponse<String> response = sendChatRequest(payload);
        if (isHttpError(response) && jsonObject) {
            // Retry without json_object mode
            payload.remove("response_format");
            response = sendChatRequest(payload);
        }
        if (isHttpError(response)) {
            throw new IOException("AI HTTP " + response.statusCode() + ": " + extractErrorMessage(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        String content = choice.path("message").path("content").asText();
        if (!StringUtils.hasText(content)) {
            throw new IOException("AI response content is empty, finish_reason=" + choice.path("finish_reason").asText("unknown"));
        }
        return content;
    }

    /**
     * Max retry attempts for transient HTTP errors (5xx).
     */
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1000;

    private HttpResponse<String> sendChatRequest(Map<String, Object> payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(chatCompletionsUri())
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        IOException lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                // Only retry on server errors (5xx)
                if (response.statusCode() >= 500 && attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                    continue;
                }
                return response;
            } catch (IOException | InterruptedException e) {
                lastException = e instanceof IOException ? (IOException) e : new IOException(e);
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                }
            }
        }
        throw lastException != null ? lastException : new IOException("AI API request failed after " + (MAX_RETRIES + 1) + " attempts");
    }

    private URI chatCompletionsUri() {
        String baseUrl = properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/chat/completions") || baseUrl.endsWith("/chat/completions/")) {
            return URI.create(baseUrl);
        }
        String separator = baseUrl.endsWith("/") ? "" : "/";
        return URI.create(baseUrl + separator + "chat/completions");
    }

    private boolean isHttpError(HttpResponse<String> response) {
        return response.statusCode() < 200 || response.statusCode() >= 300;
    }

    private String extractErrorMessage(String body) {
        try {
            String message = objectMapper.readTree(body).path("error").path("message").asText();
            if (StringUtils.hasText(message)) {
                return removeApiKeyHint(message);
            }
        } catch (JsonProcessingException ignored) {
            // Use the raw body below.
        }
        return removeApiKeyHint(body);
    }

    private String removeApiKeyHint(String message) {
        return message.replaceAll("(?i),?\\s*Your api key:[^,}]+", "").trim();
    }
}
