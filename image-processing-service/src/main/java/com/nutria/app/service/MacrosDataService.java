package com.nutria.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoTimeoutException;
import com.nutria.app.model.MacrosData;
import com.nutria.app.repository.MacrosDataRepository;
import com.nutria.common.exceptions.ResourceNotFoundException;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MacrosDataService {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([0-9]+\\.?[0-9]*)");
    private static final double DEFAULT_NUMERIC_VALUE = 0.0;

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final MacrosDataRepository macrosDataRepository;

    /**
     * Retrieves all macro data entries
     */
    public List<MacrosData> getAll() {
        return macrosDataRepository.findAll();
    }

    /**
     * Retrieves macro data by ID
     */
    public MacrosData getById(String id) {
        return macrosDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food data not found with id: " + id));
    }

    /**
     * Legacy method - maintained for backward compatibility
     * @param imageBase64 Base64 encoded image
     * @param useMockData 0 = use mock data, 1 = use AI analysis
     * @param temperature AI model temperature
     */
    public MacrosData getMacrosFromImage(String imageBase64, int useMockData, double temperature) {
        return analyzeFoodImage(imageBase64, useMockData == 0, null, temperature);
    }

    /**
     * Analyzes food image and extracts macro nutritional information
     * @param imageBase64 Base64 encoded image
     * @param useMockData Whether to use mock data for testing
     * @param userServingSize Optional user-specified serving size in grams
     * @param temperature AI model temperature (0.0 to 2.0)
     */
    public MacrosData analyzeFoodImage(String imageBase64, boolean useMockData,
                                       Double userServingSize, double temperature) {
        validateAnalysisInputs(imageBase64, temperature);

        if (useMockData) {
            return generateMockMacrosData();
        }

        return performAiAnalysis(imageBase64, userServingSize, temperature);
    }

    /**
     * Saves macro data for a user
     */
    public MacrosData saveMacros(String token, MacrosData macrosData) {
        try {
            // Extract user ID from JWT token
            Long userId = jwtService.extractId(token);
            macrosData.setUserId(userId);
            macrosData.setStatus(MacrosData.MacrosStatus.ACTIVE.getCode());

            // Validate nutritional values before saving
            macrosData.validateNutritionalValues();

            MacrosData savedData = macrosDataRepository.save(macrosData);
            log.info("Saved macro data for user: {}, food: {}", userId, macrosData.getDescription());

            return savedData;
        } catch (MongoTimeoutException e) {
            log.error("Database timeout while saving macro data", e);
            throw new ValidationException("Could not connect to the database. Please try again later.");
        } catch (Exception e) {
            log.error("Error saving macro data", e);
            throw new ValidationException("Failed to save macro data: " + e.getMessage());
        }
    }

    /**
     * Soft deletes macro data by setting status to INACTIVE
     */
    public MacrosData delete(String id) {
        MacrosData macrosData = getById(id);
        macrosData.markAsInactive();

        MacrosData deletedData = macrosDataRepository.save(macrosData);
        log.info("Soft deleted macro data with id: {}", id);

        return deletedData;
    }

    /**
     * Updates existing macro data
     */
    public MacrosData updateMacrosData(String id, MacrosData updatedData) {
        MacrosData existingData = getById(id);

        // Update fields if provided
        Optional.ofNullable(updatedData.getDescription()).ifPresent(existingData::setDescription);
        Optional.ofNullable(updatedData.getCalories()).ifPresent(existingData::setCalories);
        Optional.ofNullable(updatedData.getProteins()).ifPresent(existingData::setProteins);
        Optional.ofNullable(updatedData.getCarbs()).ifPresent(existingData::setCarbs);
        Optional.ofNullable(updatedData.getFats()).ifPresent(existingData::setFats);
        Optional.ofNullable(updatedData.getServingSize()).ifPresent(existingData::setServingSize);

        // Validate before saving
        existingData.validateNutritionalValues();

        MacrosData savedData = macrosDataRepository.save(existingData);
        log.info("Updated macro data with id: {}", id);

        return savedData;
    }

    // Private helper methods

    private void validateAnalysisInputs(String imageBase64, double temperature) {
        if (imageBase64 == null || imageBase64.trim().isEmpty()) {
            throw new ValidationException("Image data cannot be null or empty");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new ValidationException("Temperature must be between 0.0 and 2.0");
        }
    }

    private MacrosData performAiAnalysis(String imageBase64, Double userServingSize, double temperature) {
        try {
            // Call the new AiService.analyzeFood method
            Map<String, Object> aiResponse = aiService.analyzeFood(imageBase64, userServingSize, temperature);

            // Check if analysis was successful
            Boolean success = (Boolean) aiResponse.get("success");
            if (!Boolean.TRUE.equals(success)) {
                String error = (String) aiResponse.get("error");
                throw new ValidationException("AI analysis failed: " + error);
            }

            return convertAiResponseToMacrosData(aiResponse);
        } catch (Exception e) {
            log.error("AI analysis failed for image", e);
            throw new ValidationException("Failed to analyze food image: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private MacrosData convertAiResponseToMacrosData(Map<String, Object> aiResponse) {
        try {
            // Extract main fields
            String description = extractStringValue(aiResponse, "description");
            Double servingSize = extractNumericValue(aiResponse, "serving_size");

            // Extract macros data from nested structure
            Map<String, Object> macrosMap = (Map<String, Object>) aiResponse.get("macros");
            if (macrosMap == null) {
                throw new ValidationException("No macro data found in AI response");
            }

            MacrosData macrosData = new MacrosData();
            macrosData.setDescription(description);
            macrosData.setServingSize(servingSize);
            macrosData.setCalories(extractNumericValue(macrosMap, "calories"));
            macrosData.setProteins(extractNumericValue(macrosMap, "proteins"));
            macrosData.setCarbs(extractNumericValue(macrosMap, "carbohydrates"));
            macrosData.setFats(extractNumericValue(macrosMap, "fats"));

            // Validate the extracted data
            validateMacrosData(macrosData);

            log.info("Successfully converted AI response to MacrosData: {}", description);
            return macrosData;
        } catch (Exception e) {
            log.error("Error converting AI response to MacrosData", e);
            throw new ValidationException("Invalid data format received from AI service: " + e.getMessage());
        }
    }

    private String extractStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value).trim() : "";
    }

    private Double extractNumericValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return parseNumericValue(value);
    }

    private Double parseNumericValue(Object value) {
        if (value == null) {
            return DEFAULT_NUMERIC_VALUE;
        }

        // Handle direct numeric types
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        // Handle string representations
        String stringValue = String.valueOf(value).trim();
        if (stringValue.isEmpty()) {
            return DEFAULT_NUMERIC_VALUE;
        }

        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException e) {
            // Try to extract numeric value using regex
            Matcher matcher = NUMERIC_PATTERN.matcher(stringValue);
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException ex) {
                    log.warn("Could not parse numeric value from: {}", stringValue);
                    return DEFAULT_NUMERIC_VALUE;
                }
            }
            log.warn("No numeric value found in: {}", stringValue);
            return DEFAULT_NUMERIC_VALUE;
        }
    }

    private void validateMacrosData(MacrosData macrosData) {
        if (macrosData.getDescription() == null || macrosData.getDescription().trim().isEmpty()) {
            throw new ValidationException("Food description cannot be empty");
        }

        // Use the model's validation method
        macrosData.validateNutritionalValues();
    }

    /**
     * Generates mock data for testing purposes
     */
    private MacrosData generateMockMacrosData() {
        Random random = new Random();
        MockFoodType[] foodTypes = MockFoodType.values();
        MockFoodType selectedType = foodTypes[random.nextInt(foodTypes.length)];

        MacrosData mockData = new MacrosData();
        mockData.setDescription(selectedType.getDescription());
        mockData.setCalories(selectedType.getBaseCalories() + (random.nextDouble() * selectedType.getCalorieVariation()));
        mockData.setProteins(selectedType.getBaseProteins() + (random.nextDouble() * selectedType.getProteinVariation()));
        mockData.setCarbs(selectedType.getBaseCarbs() + (random.nextDouble() * selectedType.getCarbVariation()));
        mockData.setFats(selectedType.getBaseFats() + (random.nextDouble() * selectedType.getFatVariation()));
        mockData.setServingSize(selectedType.getServingSize());

        log.info("Generated mock data for: {}", selectedType.getDescription());
        return mockData;
    }

    /**
     * Enum for mock food types used in testing
     */
    private enum MockFoodType {
        HAMBURGER("Hamburguesa de carne", 250.0, 50.0, 15.0, 5.0, 25.0, 5.0, 10.0, 5.0, 100.0),
        CHICKEN_BREAST("Pechuga de pollo a la plancha", 165.0, 0.0, 31.0, 0.0, 0.0, 0.0, 3.6, 0.0, 100.0),
        WHITE_RICE("Arroz blanco cocido", 200.0, 0.0, 4.0, 0.0, 45.0, 0.0, 0.4, 0.0, 150.0),
        AVOCADO("Aguacate", 160.0, 0.0, 2.0, 0.0, 9.0, 0.0, 15.0, 0.0, 70.0),
        PEPPERONI_PIZZA("Pizza de pepperoni", 280.0, 70.0, 12.0, 3.0, 30.0, 5.0, 12.0, 6.0, 100.0),
        BAKED_SALMON("Salmón al horno", 200.0, 0.0, 20.0, 0.0, 0.0, 0.0, 13.0, 0.0, 100.0),
        OATMEAL("Avena en hojuelas", 150.0, 0.0, 5.0, 0.0, 27.0, 0.0, 3.0, 0.0, 40.0);

        private final String description;
        private final double baseCalories, calorieVariation;
        private final double baseProteins, proteinVariation;
        private final double baseCarbs, carbVariation;
        private final double baseFats, fatVariation;
        private final double servingSize;

        MockFoodType(String description, double baseCalories, double calorieVariation,
                     double baseProteins, double proteinVariation, double baseCarbs, double carbVariation,
                     double baseFats, double fatVariation, double servingSize) {
            this.description = description;
            this.baseCalories = baseCalories;
            this.calorieVariation = calorieVariation;
            this.baseProteins = baseProteins;
            this.proteinVariation = proteinVariation;
            this.baseCarbs = baseCarbs;
            this.carbVariation = carbVariation;
            this.baseFats = baseFats;
            this.fatVariation = fatVariation;
            this.servingSize = servingSize;
        }

        // Getters
        public String getDescription() { return description; }
        public double getBaseCalories() { return baseCalories; }
        public double getCalorieVariation() { return calorieVariation; }
        public double getBaseProteins() { return baseProteins; }
        public double getProteinVariation() { return proteinVariation; }
        public double getBaseCarbs() { return baseCarbs; }
        public double getCarbVariation() { return carbVariation; }
        public double getBaseFats() { return baseFats; }
        public double getFatVariation() { return fatVariation; }
        public double getServingSize() { return servingSize; }
    }
}