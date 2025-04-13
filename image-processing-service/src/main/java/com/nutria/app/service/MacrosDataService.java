package com.nutria.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoTimeoutException;
import com.nutria.app.model.MacrosData;
import com.nutria.app.repository.MacrosDataRepository;
import com.nutria.common.exceptions.ResourceNotFoundException;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

    public MacrosData getMacrosFromImage(String imageBase64, int i) {
        if (i == 0) {
            MacrosData dummyData = new MacrosData();
            dummyData.setDescription("Dummy Food");
            dummyData.setCalories(100.0);
            dummyData.setProteins(10.0);
            dummyData.setCarbs(20.0);
            dummyData.setFats(5.0);
            dummyData.setServingSize(50.0);
            return dummyData;
        }

        try {
            String aiResponse = aiService.getAnswer(imageBase64);

            Map parsedData = objectMapper.readValue(aiResponse, Map.class);

            MacrosData macrosData = new MacrosData();
            macrosData.setDescription((String) parsedData.get("description"));
            macrosData.setCalories(parseNumericValue((String) parsedData.get("calories")));
            macrosData.setProteins(parseNumericValue((String) parsedData.get("proteins")));
            macrosData.setCarbs(parseNumericValue((String) parsedData.get("carbohydrates")));
            macrosData.setFats(parseNumericValue((String) parsedData.get("fats")));
            macrosData.setServingSize(parseNumericValue((String) parsedData.get("serving_size")));
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


    private double parseNumericValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0.0;
        }

        // Extract the numeric part at the beginning of the string
        String numericPart = value.replaceAll("[^0-9.]", "").trim();
        try {
            return Double.parseDouble(numericPart);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public MacrosData delete(String id) {
        MacrosData macrosData = macrosDataRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Food data not found"));
        macrosData.setStatus(MacrosData.MacrosStatus.INACTIVE.getCode());
        return macrosDataRepository.save(macrosData);
    }

}

