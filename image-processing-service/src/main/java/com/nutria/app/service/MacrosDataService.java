package com.nutria.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MacrosDataService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final MacrosDataRepository macrosDataRepository;

    public List<MacrosData> getAll() { return macrosDataRepository.findAll(); }

    public MacrosData getById(String id) {
        return macrosDataRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Food data not found"));
    }

    public MacrosData getMacrosFromImage(String imageBase64, int i, double temperature) {
        if (i == 0) {
            Random random = new Random();
            int foodType = random.nextInt(7); // Genera un número aleatorio entre 0 y 6

            MacrosData dummyData = new MacrosData();

            switch (foodType) {
                case 0: // Hamburguesa
                    dummyData.setDescription("Hamburguesa de carne");
                    dummyData.setCalories(250 + random.nextInt(50)); // 250-300 kcal
                    dummyData.setProteins(15 + random.nextInt(5));   // 15-20 g
                    dummyData.setCarbs(25 + random.nextInt(5));      // 25-30 g
                    dummyData.setFats(10 + random.nextInt(5));       // 10-15 g
                    dummyData.setServingSize(100.0);
                    break;
                case 1: // Pechuga de pollo
                    dummyData.setDescription("Pechuga de pollo a la plancha");
                    dummyData.setCalories(165.0);
                    dummyData.setProteins(31.0);
                    dummyData.setCarbs(0.0);
                    dummyData.setFats(3.6);
                    dummyData.setServingSize(100.0);
                    break;
                case 2: // Arroz blanco
                    dummyData.setDescription("Arroz blanco cocido");
                    dummyData.setCalories(200.0);
                    dummyData.setProteins(4.0);
                    dummyData.setCarbs(45.0);
                    dummyData.setFats(0.4);
                    dummyData.setServingSize(150.0);
                    break;
                case 3: // Aguacate
                    dummyData.setDescription("Aguacate");
                    dummyData.setCalories(160.0);
                    dummyData.setProteins(2.0);
                    dummyData.setCarbs(9.0);
                    dummyData.setFats(15.0);
                    dummyData.setServingSize(70.0);
                    break;
                case 4: // Pizza de pepperoni
                    dummyData.setDescription("Pizza de pepperoni");
                    dummyData.setCalories(280 + random.nextInt(70)); // 280-350 kcal
                    dummyData.setProteins(12 + random.nextInt(3));   // 12-15 g
                    dummyData.setCarbs(30 + random.nextInt(5));      // 30-35 g
                    dummyData.setFats(12 + random.nextInt(6));       // 12-18 g
                    dummyData.setServingSize(100.0);
                    break;
                case 5: // Salmón
                    dummyData.setDescription("Salmón al horno");
                    dummyData.setCalories(200.0);
                    dummyData.setProteins(20.0);
                    dummyData.setCarbs(0.0);
                    dummyData.setFats(13.0);
                    dummyData.setServingSize(100.0);
                    break;
                case 6: // Avena
                    dummyData.setDescription("Avena en hojuelas");
                    dummyData.setCalories(150.0);
                    dummyData.setProteins(5.0);
                    dummyData.setCarbs(27.0);
                    dummyData.setFats(3.0);
                    dummyData.setServingSize(40.0);
                    break;
                default: // Por si acaso
                    dummyData.setDescription("Comida genérica");
                    dummyData.setCalories(200.0);
                    dummyData.setProteins(10.0);
                    dummyData.setCarbs(20.0);
                    dummyData.setFats(8.0);
                    dummyData.setServingSize(100.0);
                    break;
            }
            return dummyData;
        }

        try {
            String aiResponse = aiService.getAnswer(imageBase64, temperature);

            Map parsedData = objectMapper.readValue(aiResponse, Map.class);

            MacrosData macrosData = new MacrosData();
            macrosData.setDescription(String.valueOf(parsedData.get("description")));
            macrosData.setCalories(parseNumericValue(String.valueOf(parsedData.get("calories"))));
            macrosData.setProteins(parseNumericValue(String.valueOf(parsedData.get("proteins"))));
            macrosData.setCarbs(parseNumericValue(String.valueOf(parsedData.get("carbohydrates"))));
            macrosData.setFats(parseNumericValue(String.valueOf(parsedData.get("fats"))));
            macrosData.setServingSize(parseNumericValue(String.valueOf(parsedData.get("serving_size"))));
            return macrosData;

        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid data format received from AI service");
        }
    }

    public void saveMacros(String token, MacrosData macrosData) {
        try {
            macrosData.setUserId(jwtService.extractId(token));
            macrosData.setStatus(MacrosData.MacrosStatus.ACTIVE.getCode());
            macrosDataRepository.save(macrosData);

        } catch (MongoTimeoutException e) {
            throw new ValidationException("Could not connect to the database. Please try again later.");
        }
    }


    private double parseNumericValue(Object value) {
        if (value == null) {
            return 0.0;
        }

        String stringValue = String.valueOf(value);
        if (stringValue.isEmpty()) {
            return 0.0;
        }

        try {
            // If it's a clean number, parse directly
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException e) {
            // If not, try to extract a valid number
            Matcher matcher = Pattern.compile("([0-9]+\\.?[0-9]*)").matcher(stringValue);
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException ex) {
                    return 0.0;
                }
            }
            return 0.0;
        }
    }

    public MacrosData delete(String id) {
        MacrosData macrosData = macrosDataRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Food data not found"));
        macrosData.setStatus(MacrosData.MacrosStatus.INACTIVE.getCode());
        return macrosDataRepository.save(macrosData);
    }

}

