package com.nutria.app.service;

import com.nutria.app.dto.MacrosData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTraceService {

    private final RestTemplate restTemplate;
    private final WebClient.Builder webClientBuilder;

    public List<?> fetchMacrosData(String token, LocalDate initDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-d");
        String formattedInitDate = initDate.format(formatter);
        String formattedEndDate = endDate.format(formatter);

        String url = "/api/v1/food-trace/find?init=" + formattedInitDate + "&end=" + formattedEndDate;

        try {
            // First, get the response as a Map
            Map response = webClientBuilder.build()
                    .get()
                    .uri("http://image-processing-service" + url)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("data") == null) {
                log.warn("No data returned from food trace service");
                return new ArrayList<>();
            }

            // Extract the data array
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");

            // Extract just the macrosData into a list
            List<MacrosData> macrosDataList = new ArrayList<>();
            for (Map<String, Object> item : dataList) {
                Map<String, Object> macrosDataMap = (Map<String, Object>) item.get("macrosData");
                if (macrosDataMap != null) {
                    MacrosData macrosData = convertToMacrosData(macrosDataMap);
                    macrosDataList.add(macrosData);
                }
            }

            return macrosDataList;

        } catch (Exception e) {
            log.error("Error fetching macros data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private MacrosData convertToMacrosData(Map<String, Object> map) {
        MacrosData macrosData = new MacrosData();
        macrosData.setCalories(((Number) map.get("calories")).doubleValue());
        macrosData.setProteins(((Number) map.get("proteins")).doubleValue());
        macrosData.setCarbs(((Number) map.get("carbs")).doubleValue());
        macrosData.setFats(((Number) map.get("fats")).doubleValue());
        return macrosData;
    }

}