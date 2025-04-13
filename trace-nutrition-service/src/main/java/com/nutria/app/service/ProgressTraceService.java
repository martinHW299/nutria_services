package com.nutria.app.service;

import com.nutria.app.dto.MacrosData;
import com.nutria.app.model.*;
import com.nutria.app.repository.*;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTraceService {

    private final JwtService jwtService;
    private final WebClient.Builder webClientBuilder;
    private final WeightTraceService weightTraceService;
    private final ProgressTraceRepository progressTraceRepository;

    public List<MacrosData> fetchMacrosData(String token, Date initDate, Date endDate) {
        if (initDate.after(endDate)) {
            throw new ValidationException("End periodic date cannot be before initial date");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedInitDate = dateFormat.format(initDate);
        String formattedEndDate = dateFormat.format(endDate);

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

            if (response == null) {
                throw new ServiceException("Null response received from image-processing-service");
            }

            if (response.get("data") == null) {
                return Collections.emptyList();
            }

            // Extract the data array with proper type checking
            List<MacrosData> macrosDataList = new ArrayList<>();
            if (response.get("data") instanceof List<?> dataList) {
                for (Object item : dataList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        Object macrosObj = itemMap.get("macrosData");

                        if (macrosObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> macrosDataMap = (Map<String, Object>) macrosObj;
                            MacrosData macrosData = convertToMacrosData(macrosDataMap);
                            macrosDataList.add(macrosData);
                        }
                    }
                }
            }

            return macrosDataList;

        } catch (WebClientResponseException e) {
            throw new ServiceException("Error fetching macros data: " + e.getMessage(), e);
        } catch (WebClientException e) {
            throw new ServiceException("Error connecting to macros data service", e);
        } catch (Exception e) {
            throw new ServiceException("Unexpected error fetching macros data: " + e.getMessage(), e);
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


    public ProgressTrace savePeriodicProgressTrace(String token, Date initDate, Date endDate) {

        List<MacrosData> macrosDataList = fetchMacrosData(token, initDate, endDate);
        Long userId = jwtService.extractId(token);

        double consumedCalories = 0.0;
        double consumedProteins = 0.0;
        double consumedCarbs = 0.0;
        double consumedFats = 0.0;

        for (MacrosData macrosData : macrosDataList) {
            consumedCalories += macrosData.getCalories();
            consumedProteins += macrosData.getProteins();
            consumedCarbs += macrosData.getCarbs();
            consumedFats += macrosData.getFats();
        }

        LocalDate initLocalDate = initDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endtLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long daysDifference = ChronoUnit.DAYS.between(initLocalDate, endtLocalDate);

        WeightTrace weightTrace = weightTraceService.saveWeightTrace(token, daysDifference, consumedCalories);
        log.info("weightTrace: {}", weightTrace);

        ProgressTrace progressTrace = ProgressTrace.builder()
                .userId(userId)
                .dateInit(initDate)
                .dateEnt(endDate)
                .caloriesConsumed(consumedCalories)
                .proteinsConsumed(consumedProteins)
                .carbsConsumed(consumedCarbs)
                .fatsConsumed(consumedFats)
                .weightTrace(weightTrace)
                .build();

        log.info("progressTrace: {}", progressTrace);

        return progressTraceRepository.save(progressTrace);

    }


}