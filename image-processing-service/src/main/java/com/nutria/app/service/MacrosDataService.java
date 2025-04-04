package com.nutria.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutria.app.model.MacrosData;
import com.nutria.app.repository.MacrosDataRepository;
import com.nutria.common.exceptions.ResourceNotFoundException;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MacrosDataService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final MacrosDataRepository macrosDataRepository;

    public List<MacrosData> getAll() { return macrosDataRepository.findAll(); }

    public MacrosData getById(String id) {
        return macrosDataRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Food data not found"));
    }

    public MacrosData saveMacros(Long userId, String imageBase64) {
        try {
            String aiResponse = aiService.getAnswer(imageBase64);

            Map parsedData = objectMapper.readValue(aiResponse, Map.class);

            MacrosData macrosData = new MacrosData();
            macrosData.setUserId(userId);
            macrosData.setDescription((String) parsedData.get("description"));
            macrosData.setCalories(parseNumericValue((String) parsedData.get("calories")));
            macrosData.setProteins(parseNumericValue((String) parsedData.get("proteins")));
            macrosData.setCarbs(parseNumericValue((String) parsedData.get("carbohydrates")));
            macrosData.setFats(parseNumericValue((String) parsedData.get("fats")));
            macrosData.setServingSize(parseNumericValue((String) parsedData.get("serving_size")));
            macrosData.setStatus(MacrosData.MacrosStatus.ACTIVE.getCode());
            return macrosDataRepository.save(macrosData);
        } catch (IOException e) {
            throw new ValidationException("Failed to save food macros data");
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
