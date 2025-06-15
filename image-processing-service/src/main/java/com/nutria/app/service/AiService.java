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
//        log.info("response: {}", response);

//        Extract text from response
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
                Analyze the food image with expertise in nutrition and provide a detailed breakdown of its macronutrients in a JSON object.\s
                The output should include the following fields:\s
                   - 'description': A **brief, one-line** description in **Spanish**, stating what the food is, without details about cooking method, texture, or visual presentation.\s
                   - 'calories': The total caloric content in kilocalories (kcal), using real-world nutrition values and avoiding arbitrary rounding.\s
                   - 'proteins': The protein content in grams (g).\s
                   - 'carbohydrates': The carbohydrate content in grams (g).\s
                   - 'fats': The fat content in grams (g).\s
                   - 'serving_size': An estimated serving size in grams (g), based on a realistic assessment of the image.
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
                        "topP", 1.0,
                        "maxOutputTokens", 800,
                        "response_mime_type", "application/json"
                )
        );
    }
}
