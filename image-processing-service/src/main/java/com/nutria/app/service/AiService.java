package com.nutria.app.service;

import com.nutria.common.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;

    public AiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getAnswer(String image64) {
        // Build request payload
        Map<String, Object> request = buildGeminiPayload(image64);

        // Make the API call
        Map<String, Object> response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        // Extract text from response
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");

                if (content != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            throw new ValidationException(e.getMessage());
        }
        return null;
    }

    private Map<String, Object> buildGeminiPayload(String image64) {
        String prompt = "Provide a detailed analysis of all the following attributes with the corresponding unit: description, calories (Kcal), proteins (g), carbohydrates (g), fats (g), serving size (g), in JSON format." +
                "In case the image does not contain any food item, then return a null response";

        return Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", prompt),
                                        Map.of(
                                                "inline_data", Map.of(
                                                        "mime_type", "image/png",
                                                        "data", image64
                                                )
                                        )
                                }
                        )
                },
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "response_mime_type", "application/json",
                        "maxOutputTokens", 800
                )
        );
    }
}
