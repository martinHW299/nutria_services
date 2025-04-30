package com.nutria.app.service;

import com.nutria.app.dto.MacrosData;
import com.nutria.app.dto.MacrosSummary;
import com.nutria.app.repository.*;
import com.nutria.app.utilities.DateUtils;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.SimpleDateFormat;
import java.time.Instant;
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
    private final DateUtils dateUtils;

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


    public MacrosSummary fetchDailyMacrosData(String token, Date date) {
        List<MacrosData> macros = fetchMacrosData(token, date, date);
        return calculateSummary(token, date.toString(), macros, date, date);
    }


    public List<MacrosSummary> fetchWeeklyMacrosData(String token, Date initDate) {
        if (initDate == null) {
            throw new RuntimeException("Date not found");
        }

        Date firstDayWeek = dateUtils.getFirstDayOfWeek(initDate);
        List<MacrosSummary> dailySummaries = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstDayWeek);

        String[] dayFlags = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

        for (int i = 0; i < 7; i++) {
            Date day = calendar.getTime();

            // You might want to fetch data only for that single day
            List<MacrosData> dayData = fetchMacrosData(token, day, day);

            String flag = dayFlags[i];
            MacrosSummary summary = calculateSummary(token, flag, dayData, day, day);
            dailySummaries.add(summary);

            calendar.add(Calendar.DATE, 1); // move to next day
        }

        return dailySummaries;
    }

    public List<MacrosSummary> fetchMonthlyMacrosData(String token, Date inputDate) {

        List<MacrosSummary> weeklySummaries = new ArrayList<>();

        if (inputDate == null) {
            throw new RuntimeException("Date not found");
        }

        Date firstDayMonth = dateUtils.getFirstDayOfMonth(inputDate);
        Date lastDayMonth = dateUtils.getLastDayOfMonth(inputDate);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(firstDayMonth);
        int i = 1;


        while (!calendar.getTime().after(lastDayMonth)) {
            Date firstDayWeek = dateUtils.getFirstDayOfWeek(calendar.getTime());
            Date lastDayWeek = dateUtils.getLastDayOfWeek(calendar.getTime());

            if (firstDayWeek.before(firstDayMonth)) {
                firstDayWeek = firstDayMonth;
            }
            if (lastDayWeek.after(lastDayMonth)) {
                lastDayWeek = lastDayMonth;
            }

            List<MacrosData> weekData = fetchMacrosData(token, firstDayWeek, lastDayWeek);
            MacrosSummary summary = calculateSummary(token, "week "+ i, weekData, firstDayWeek, lastDayWeek);
            weeklySummaries.add(summary);

            calendar.setTime(lastDayWeek);
            calendar.add(Calendar.DATE, 1);
            i++;
        }

        return weeklySummaries;
    }

    private MacrosSummary calculateSummary(String token, String flag, List<MacrosData> macrosDataList, Date firstDayWeek, Date lastDayWeek) {

        LocalDate startDate = Instant.ofEpochMilli(firstDayWeek.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(lastDayWeek.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        long daysDifference = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        log.info("daysDifference: {}", daysDifference);

        double totalCalories = 0;
        double totalProteins = 0;
        double totalCarbs = 0;
        double totalFats = 0;
        double weightTrace = 0;
        boolean gotRecords = true;

        if (macrosDataList.isEmpty()) {
            gotRecords = false;
        } else {
            for (MacrosData data : macrosDataList) {
                totalCalories += data.getCalories();
                totalProteins += data.getProteins();
                totalCarbs += data.getCarbs();
                totalFats += data.getFats();
            }
            weightTrace = weightTraceService.weightChangeByIntake(token, daysDifference, totalCalories);
        }

        return MacrosSummary.builder()
                .tag(flag)
                .totalCalories(totalCalories)
                .totalProteins(totalProteins)
                .totalCarbs(totalCarbs)
                .totalFats(totalFats)
                .weightStatus(weightTrace)
                .gotRecords(gotRecords)
                .dateInit(firstDayWeek)
                .dateEnd(lastDayWeek)
                .build();
    }
}