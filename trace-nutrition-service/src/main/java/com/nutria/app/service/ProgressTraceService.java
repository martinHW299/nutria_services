package com.nutria.app.service;

import com.nutria.app.dto.MacrosData;
import com.nutria.app.model.ProgressTrace;
import com.nutria.app.model.WeightTrace;
import com.nutria.app.repository.ProgressTraceRepository;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTraceService {

    private final JwtService jwtService;
    private final WebClient.Builder webClientBuilder;
    private final WeightTraceService weightTraceService;
    private final ProgressTraceRepository progressTraceRepository;

    public List<MacrosData> fetchMacrosData(String token, Date initDate, Date endDate) {
        try {
            if (initDate.after(endDate)) {
                throw new ValidationException("End periodic date can not be before initial date");
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            String formattedInitDate = dateFormat.format(initDate);
            String formattedEndDate = dateFormat.format(endDate);

            String url = "/api/v1/food-trace/find?init=" + formattedInitDate + "&end=" + formattedEndDate;
            log.info("url: {}", url);
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

        return progressTraceRepository.save(progressTrace);

    }


}