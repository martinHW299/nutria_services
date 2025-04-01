package com.nutria.app.service;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutria.app.dto.MacrosDataRequest;
import com.nutria.app.model.MacrosData;
import com.nutria.app.repository.MacrosDataRepository;
import com.nutria.common.exceptions.ValidationException;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MacrosDataService {

    private final AiService aiService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final MacrosDataRepository macrosDataRepository;

    public List<MacrosData> getAll() { return macrosDataRepository.findAll(); }

    public Optional<MacrosData> getById(String id) { return macrosDataRepository.findById(id); }

    public MacrosData saveMacros(String token, String imageBase64) throws JsonProcessingException {

        Long userId = jwtService.extractId(token);
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

    public void delete(String id) { macrosDataRepository.deleteById(id); }

}
