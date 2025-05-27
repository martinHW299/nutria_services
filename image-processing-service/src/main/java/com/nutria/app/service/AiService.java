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

    public String getAnswer(String image64, double temperature) {
        // Build request payload
        Map<String, Object> request = buildGeminiPayload(image64, temperature);

        // Make the API call
        Map<String, Object> response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        log.info("response: {}", response);

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

    private Map<String, Object> buildGeminiPayload(String image64, double temperature) {
        String prompt = """
                You are a nutrition analysis expert. Carefully analyze the food shown in the image.
                Return a SINGLE JSON object with the following nutrient information:
                - "description": The name of the food, including any specific details about the preparation or sauce.
                - "calories": The total calories in kilocalories (kcal).
                - "proteins": The protein content in grams (g).
                - "carbohydrates": The carbohydrate content in grams (g).
                - "fats": The fat content in grams (g).
                - "serving_size": The estimated serving size in grams (g) based on the image.
                Use real-world nutrition values. Do NOT round calories to 250, 350, 500, or 550 unless they are clearly appropriate.
                Estimate serving size realistically. Avoid default numbers unless justified.
                Your response should be specific, image-based, and accurate — not generic.
                """;

        return Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", prompt),
                                        Map.of(
                                                "inline_data", Map.of(
                                                        "mime_type",
                                                        "image/png",
                                                        "data", image64
                                                )
                                        )
                                }
                        )
                },
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "response_mime_type", "application/json",
                        "maxOutputTokens", 800
                )
        );
    }
}
