package com.nutria.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.nutria.common.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AiService {

    // Constants
    private static final String DATA_IMAGE_PREFIX = "data:image/png;base64,";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final double MAX_REASONABLE_SERVING_SIZE = 2000.0;
    private static final int OPENAI_MAX_TOKENS = 300;
    private static final int GEMINI_MAX_TOKENS = 800;
    private static final double PRECISION_FACTOR = 100.0;


    // Configuration
    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.model}")
    private String openaiModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GenerativeModel generativeModel; // Make it final and private

    public AiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, GenerativeModel generativeModel) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.generativeModel = generativeModel;

        // Add validation
        if (this.generativeModel == null) {
            log.error("GenerativeModel is null - Vertex AI functionality will not work");
            throw new IllegalArgumentException("GenerativeModel cannot be null. Check your GeminiConfiguration bean setup.");
        }

        log.info("AiService initialized with GenerativeModel successfully");
    }

    public Map<String, Object> analyzeFood(String base64Image, Double userServingSize, double temperature) {
        validateInputs(base64Image, temperature);

        if (userServingSize != null) {
            return analyzeFoodWithFixedServing(base64Image, userServingSize, temperature);
        }
        return analyzeFoodWithEstimatedServing(base64Image, temperature);
    }

    // Private methods for main workflow

    private Map<String, Object> analyzeFoodWithFixedServing(String base64Image, Double userServingSize, double temperature) {
        Double sanitizedServing = sanitizeServingSize(userServingSize);

        // Optional shadow run for description hint
        String descriptionHint = getShadowDescription(base64Image);

//        FoodMacros macros = estimateMacrosWithGemini(base64Image, descriptionHint, sanitizedServing, temperature);

        FoodMacros macros = estimateMacrosWithVertexAI(base64Image, descriptionHint, sanitizedServing, temperature);

        return buildAnalysisResult(
                macros.description(),
                sanitizedServing,
                "user",
                macros,
                null,
                macros.rawResponse()
        );
    }

    private Map<String, Object> analyzeFoodWithEstimatedServing(String base64Image, double temperature) {
        FoodPortion portion = estimatePortionWithGptMini(base64Image, temperature);
//        FoodMacros macros = estimateMacrosWithGemini(base64Image, portion.description(), portion.servingSize(), temperature);
        FoodMacros macros = estimateMacrosWithVertexAI(base64Image, portion.description(), portion.servingSize(), temperature);

        return buildAnalysisResult(
                macros.description(),
                portion.servingSize(),
                "model",
                macros,
                portion.rawResponse(),
                macros.rawResponse()
        );
    }

    private String getShadowDescription(String base64Image) {
        try {
            FoodPortion shadowPortion = estimatePortionWithGptMini(base64Image, 0.0);
            return shadowPortion.description();
        } catch (Exception e) {
            log.debug("Shadow description estimation failed: {}", e.getMessage());
            return null;
        }
    }

    // OpenAI GPT-mini integration

    public FoodPortion estimatePortionWithGptMini(String base64Image, double temperature) {
        try {
            Map<String, Object> payload = buildOpenAiPayload(base64Image, temperature);
            Map<String, Object> response = callOpenAi(payload);
            log.info("estimatePortionWithGptMini: {}", payload, "Response: {}", response);
            return parseOpenAiResponse(response);
        } catch (Exception e) {
            log.error("OpenAI portion estimation failed", e);
            throw new ValidationException("Portion estimation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildOpenAiPayload(String base64Image, double temperature) {
        return Map.of(
                "model", openaiModel,
                "temperature", temperature,
                "max_tokens", OPENAI_MAX_TOKENS,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        buildSystemMessage(),
                        buildUserMessage(base64Image)
                )
        );
    }

    private Map<String, Object> buildSystemMessage() {
        return Map.of("role", "system", "content",
                """
                You are an expert nutritionist-visual analyst. From the image, identify the main food 
                (short English name) and estimate ONE standard serving weight in grams.
                Use reliable scale cues (plate/utensils/hands/container), food state (raw/cooked/unknown) 
                and density (liquid/semi-solid/solid/leafy).
                Convert visible quantity to mL (unit map or geometry) before grams using realistic density priors. 
                Respond with JSON only.
                """);
    }

    private Map<String, Object> buildUserMessage(String base64Image) {
        return Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", buildUserPrompt()),
                        Map.of("type", "image_url", "image_url",
                                Map.of("url", DATA_IMAGE_PREFIX + base64Image))
                )
        );
    }

    private String buildUserPrompt() {
        return """
        Analyze the image and identify ALL visible food components. For each food item, estimate its serving size in grams.
        Think step by step but do NOT show your reasoning in the output.

        ### ANALYSIS APPROACH (internal use only)
        1. Scan the entire image for all food items (main dishes, sides, sauces, garnishes, drinks)
        2. Estimate portion size for each component using visual cues
        3. Use scale references: plate (26-28cm), utensils, hands, containers
        4. Consider food state: raw/cooked affects density and weight
        5. Apply realistic portion sizes for meal context

        ### MEASUREMENT GUIDELINES (do not output)
        Volume conversions: cup=240mL, tbsp=15mL, tsp=5mL, bowl≈400mL
        Density estimates:
        - Liquids: ≈240g/cup (water, juices, broths)
        - Semi-solids: ≈240±40g/cup (yogurt, sauces, purees)
        - Cooked grains: ≈150-180g/cup (rice, pasta, quinoa)
        - Leafy vegetables: ≈25-40g/cup (lettuce, spinach, herbs)
        - Diced vegetables: ≈130-170g/cup (carrots, onions, peppers)
        - Proteins: ≈200-250g/cup (meat, fish, chicken pieces)
        - Nuts/seeds: ≈140-160g/cup
        
        Portion estimation tips:
        - Palm size ≈ 80-120g protein
        - Fist size ≈ 200-300g vegetables/fruits
        - Thumb size ≈ 15-30g fats/oils
        - Cupped hand ≈ 150-200g grains/starches

        ### OUTPUT FORMAT (STRICT JSON ONLY)
        If SINGLE food item detected:
        {"description":"<food name in English>","serving_size":<float>}
        
        If MULTIPLE food items detected:
        {
          "description":"<primary dish name>",
          "serving_size":<total_weight_float>,
          "components":[
            {"name":"<component1_name>","serving_size":<float>},
            {"name":"<component2_name>","serving_size":<float>},
            {"name":"<component3_name>","serving_size":<float>}
          ]
        }

        ### EXAMPLES (for reference only)
        Single item: {"description":"grilled salmon","serving_size":150.0}
        
        Multiple items: {
          "description":"chicken rice bowl",
          "serving_size":380.0,
          "components":[
            {"name":"grilled chicken breast","serving_size":120.0},
            {"name":"white rice","serving_size":200.0},
            {"name":"steamed broccoli","serving_size":60.0}
          ]
        }

        Return ONLY the JSON response, no other text.
        """;
    }

    private Map<String, Object> callOpenAi(Map<String, Object> payload) {
        return webClient.post()
                .uri(openaiApiUrl)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + openaiApiKey)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    private FoodPortion parseOpenAiResponse(Map<String, Object> response) {
        Map<String, Object> data = extractOpenAiJson(response);
        if (data == null) {
            throw new ValidationException("No valid JSON from OpenAI");
        }

        String description = (String) data.get("description");
        if (description == null || description.isBlank()) {
            throw new ValidationException("Missing food description");
        }

        Double servingSize = Optional.ofNullable(data.get("serving_size"))
                .map(Number.class::cast)
                .map(Number::doubleValue)
                .orElse(null);

        // Handle multiple components if present
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) data.get("components");

        if (components != null && !components.isEmpty()) {
            log.info("Detected {} food components", components.size());

            // Log each component for debugging
            for (Map<String, Object> component : components) {
                String componentName = (String) component.get("name");
                Number componentSize = (Number) component.get("serving_size");
                log.info("Component: {} - {}g", componentName, componentSize);
            }

            // You could enhance this to create a more detailed description

            description = description + " (" +
                    components.stream()
                            .map(comp -> (String) comp.get("name"))
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") +
                    ")";
        }

        return new FoodPortion(description, servingSize, response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractOpenAiJson(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return null;

            String content = (String) message.get("content");
            if (content == null) return null;

            return objectMapper.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI JSON response: {}", e.getMessage());
            return null;
        }
    }

    // Gemini integration

    public FoodMacros estimateMacrosWithGemini(String base64Image, String descriptionHint,
                                               Double servingSizeGrams, double temperature) {
        try {
            Map<String, Object> payload = buildGeminiPayload(base64Image, descriptionHint, servingSizeGrams, temperature);
            Map<String, Object> response = callGemini(payload);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Gemini macros estimation failed", e);
            throw new ValidationException("Macros estimation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildGeminiPayload(String base64Image, String descriptionHint,
                                                   Double servingSizeGrams, double temperature) {
        String prompt = buildGeminiPrompt(descriptionHint, servingSizeGrams);

        return Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", "image/png",
                                        "data", base64Image))
                        })
                },
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", GEMINI_MAX_TOKENS,
                        "response_mime_type", APPLICATION_JSON
                )
        );
    }

    private String buildGeminiPrompt(String descriptionHint, Double servingSizeGrams) {
        return String.format("""
        You are an expert nutritionist. Analyze the food image and calculate precise nutritional values.
        
        GUIDANCE PROVIDED:
        - Food description: %s
        - Estimated serving size: %s grams
        
        TASK:
        Use the provided description and serving size as guidance to estimate accurate nutritional values.
        Cross-reference the image to verify the food matches the description and adjust if needed.
        
        NUTRITIONAL CALCULATION GUIDELINES:
        - Use authoritative sources: USDA FoodData Central, NHANES, ASA24, MyPlate guidelines
        - Consider cooking method, preparation style, and ingredients visible in the image
        - Account for typical recipes and ingredient proportions for this food type
        - If multiple components are visible, calculate combined nutritional values
        - Adjust for actual portion size shown vs. standard serving sizes
        
        ANALYSIS APPROACH:
        1. Verify the food description matches what you see in the image
        2. Identify all visible components (protein, carbs, fats, vegetables, sauces)
        3. Estimate the nutritional density per 100g for this specific food
        4. Scale to the provided serving size
        5. Consider cooking oils, seasonings, and preparation methods that affect nutrition
        
        QUALITY CHECKS:
        - Calories should align with macronutrient composition (protein=4 cal/g, carbs=4 cal/g, fats=9 cal/g)
        - Values should be realistic for the food type and portion size
        - Account for water content, cooking method, and ingredient ratios
        
        JSON OUTPUT (return ONLY this JSON, no other text):
        {
          "description": "accurate Spanish description of the food",
          "calories": <float>,
          "proteins": <float>,
          "carbohydrates": <float>,
          "fats": <float>,
          "serving_size": <float>
        }
        
        EXAMPLE CALCULATIONS:
        - 150g grilled chicken breast: ~248 cal, ~47g protein, ~0g carbs, ~5g fat
        - 200g white rice: ~260 cal, ~5g protein, ~53g carbs, ~1g fat
        - 100g mixed vegetables: ~25 cal, ~2g protein, ~5g carbs, ~0g fat
        
        Important: The serving_size in your response should match the provided serving size (%s grams).
        Focus on accurate nutritional analysis based on both the image and the guidance provided.
        """,
                (descriptionHint == null || descriptionHint.isBlank()) ? "not provided" : descriptionHint,
                servingSizeGrams == null ? "not provided" : servingSizeGrams.toString(),
                servingSizeGrams == null ? "standard portion" : servingSizeGrams.toString()
        );
    }

    private Map<String, Object> callGemini(Map<String, Object> payload) {
        return webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    public FoodMacros estimateMacrosWithVertexAI(String base64Image, String descriptionHint,
                                                 Double servingSizeGrams, double temperature) {
        try {
            String prompt = buildGeminiPrompt(descriptionHint, servingSizeGrams);
            log.info("Vertex AI Prompt: {}", prompt);

            // Decode base64 image to bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            log.info("Image size: {} bytes", imageBytes.length);

            // Create parts separately for better debugging
            Content content = ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData("image/png", imageBytes),
                    prompt  // Pass the prompt string directly, not as a Part
            );

            log.info("Content created successfully with {} parts", content.getPartsList().size());

            // Try without custom generation config first to see if that's the issue
            try {
                log.info("Attempting Vertex AI call without custom config...");
                GenerateContentResponse response = generativeModel.generateContent(content);
                log.info("Vertex AI response received");

                // Debug the full response
                log.info("Response candidates count: {}", response.getCandidatesCount());
                if (response.getCandidatesCount() > 0) {
                    var candidate = response.getCandidates(0);
                    log.info("First candidate content parts count: {}", candidate.getContent().getPartsCount());
                    if (candidate.getContent().getPartsCount() > 0) {
                        String responseText = candidate.getContent().getParts(0).getText();
                        log.info("Response text length: {}", responseText.length());
                        log.info("Response text preview: {}", responseText.substring(0, Math.min(200, responseText.length())));
                    }
                }

                return parseVertexAIResponse(response);

            } catch (Exception e) {
                log.error("Failed with default config, trying with custom generation config", e);

                // Fallback: try with custom generation config
                com.google.cloud.vertexai.api.GenerationConfig generationConfig =
                        com.google.cloud.vertexai.api.GenerationConfig.newBuilder()
                                .setTemperature(Math.max(0.1f, (float) temperature)) // Ensure minimum temperature
                                .setMaxOutputTokens(GEMINI_MAX_TOKENS)
                                // Remove response_mime_type for now to see if that's causing issues
                                // .setResponseMimeType(APPLICATION_JSON)
                                .build();

                GenerativeModel configuredModel = generativeModel.withGenerationConfig(generationConfig);
                GenerateContentResponse response = configuredModel.generateContent(content);

                return parseVertexAIResponse(response);
            }

        } catch (Exception e) {
            log.error("Vertex AI macros estimation failed", e);
            // Include more details in the exception
            throw new ValidationException("Macros estimation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private FoodMacros parseVertexAIResponse(GenerateContentResponse response) {
        try {
            log.info("Parsing Vertex AI response...");

            // Check if response has candidates
            if (response.getCandidatesCount() == 0) {
                log.error("No candidates in Vertex AI response");
                throw new ValidationException("No candidates in Vertex AI response");
            }

            var candidate = response.getCandidates(0);

            // Check if candidate has content
            if (!candidate.hasContent() || candidate.getContent().getPartsCount() == 0) {
                log.error("No content in Vertex AI response candidate");
                throw new ValidationException("No content in Vertex AI response candidate");
            }

            // Extract text from the first part
            String jsonText = candidate.getContent().getParts(0).getText();
            log.info("Raw response text: {}", jsonText);

            if (jsonText.isBlank()) {
                log.error("Empty text in Vertex AI response");
                throw new ValidationException("Empty text in Vertex AI response");
            }

            String cleanedJson = jsonText.trim();

            cleanedJson = cleanedJson.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .replaceAll("^```\\s*", "")
                    .trim();

            // Find JSON object if response contains extra text
            int jsonStart = cleanedJson.indexOf("{");
            int jsonEnd = cleanedJson.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleanedJson = cleanedJson.substring(jsonStart, jsonEnd + 1);
            }

            log.info("Cleaned JSON: {}", cleanedJson);

            // Parse JSON
            Map<String, Object> data;
            try {
                data = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
            } catch (Exception jsonException) {
                log.error("Failed to parse JSON: {}", cleanedJson, jsonException);
                throw new ValidationException("Failed to parse JSON response: " + jsonException.getMessage());
            }

            // Validate required fields
            if (!data.containsKey("description") || !data.containsKey("calories") ||
                    !data.containsKey("proteins") || !data.containsKey("carbohydrates") ||
                    !data.containsKey("fats") || !data.containsKey("serving_size")) {
                log.error("Missing required fields in response: {}", data.keySet());
                throw new ValidationException("Missing required nutritional fields in response");
            }

            // Create simplified rawResponse for compatibility
            Map<String, Object> rawResponse = Map.of(
                    "candidates", List.of(Map.of(
                            "content", Map.of(
                                    "parts", List.of(Map.of("text", jsonText))
                            )
                    ))
            );

            FoodMacros result = new FoodMacros(
                    (String) data.get("description"),
                    ((Number) data.get("calories")).doubleValue(),
                    ((Number) data.get("proteins")).doubleValue(),
                    ((Number) data.get("carbohydrates")).doubleValue(),
                    ((Number) data.get("fats")).doubleValue(),
                    ((Number) data.get("serving_size")).doubleValue(),
                    rawResponse
            );

            log.info("Successfully parsed FoodMacros: {}", result.description());
            return result;

        } catch (ValidationException e) {
            // Re-throw validation exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing Vertex AI response", e);
            throw new ValidationException("Failed to parse Vertex AI response: " + e.getMessage());
        }
    }

    private FoodMacros parseGeminiResponse(Map<String, Object> response) {
        String jsonText = extractTextFromGemini(response);
        if (jsonText == null || jsonText.isBlank()) {
            throw new ValidationException("Empty Gemini response");
        }

        try {
            Map<String, Object> data = objectMapper.readValue(jsonText, new TypeReference<>() {});
            return new FoodMacros(
                    (String) data.get("description"),
                    ((Number) data.get("calories")).doubleValue(),
                    ((Number) data.get("proteins")).doubleValue(),
                    ((Number) data.get("carbohydrates")).doubleValue(),
                    ((Number) data.get("fats")).doubleValue(),
                    ((Number) data.get("serving_size")).doubleValue(),
                    response
            );
        } catch (Exception e) {
            throw new ValidationException("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGemini(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            Object text = parts.get(0).get("text");
            return text == null ? null : text.toString();
        } catch (Exception e) {
            throw new ValidationException("Failed to extract text from Gemini response: " + e.getMessage());
        }
    }

    // Utility methods

    private void validateInputs(String base64Image, double temperature) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new ValidationException("Base64 image cannot be null or empty");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new ValidationException("Temperature must be between 0.0 and 2.0");
        }
    }

    private Double sanitizeServingSize(Double value) {
        if (value == null) return null;
        if (value <= 0) {
            throw new ValidationException("Serving size must be greater than 0 grams");
        }
        if (value > MAX_REASONABLE_SERVING_SIZE) {
            log.warn("Unusually large serving size: {} g", value);
        }
        return Math.round(value * PRECISION_FACTOR) / PRECISION_FACTOR;
    }

    private Map<String, Object> buildAnalysisResult(String description, Double servingSize,
                                                    String servingSource, FoodMacros macros,
                                                    Map<String, Object> portionStage,
                                                    Map<String, Object> macrosStage) {

        Map<String, Object> result = Map.of(
                "success", true,
                "description", description,
                "serving_size", servingSize,
                "serving_source", servingSource,
                "macros", Map.of(
                        "calories", macros.calories(),
                        "proteins", macros.proteins(),
                        "carbohydrates", macros.carbohydrates(),
                        "fats", macros.fats()
                )
        );

        if (portionStage != null) {
            result = Map.of(
                    "success", true,
                    "description", description,
                    "serving_size", servingSize,
                    "serving_source", servingSource,
                    "macros", Map.of(
                            "calories", macros.calories(),
                            "proteins", macros.proteins(),
                            "carbohydrates", macros.carbohydrates(),
                            "fats", macros.fats()
                    ),
                    "portion_stage", portionStage,
                    "macros_stage", macrosStage
            );
        }

        return result;
    }

    // Record classes for type safety
    private record FoodPortion(String description, Double servingSize, Map<String, Object> rawResponse) {}

    private record FoodMacros(String description, double calories, double proteins,
                              double carbohydrates, double fats, double servingSize,
                              Map<String, Object> rawResponse) {}
}