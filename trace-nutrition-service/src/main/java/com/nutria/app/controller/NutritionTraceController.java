package com.nutria.app.controller;

import com.nutria.app.dto.MacrosData;
import com.nutria.app.dto.MacrosSummary;
import com.nutria.app.model.DailyIntakeGoal;
import com.nutria.app.service.DailyIntakeGoalService;
import com.nutria.app.service.ProgressTraceService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nutrition-trace")
public class NutritionTraceController {

    private final ProgressTraceService progressTraceService;
    private final DailyIntakeGoalService dailyIntakeGoalService;

    @PostMapping("/save-goal")
    public ResponseEntity<ApiResponse<DailyIntakeGoal>> saveDailyGoal(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date
    ){
        return ResponseEntity.ok(ApiResponse.success(dailyIntakeGoalService.saveDailyIntakeGoal(token, date)));
    }

    @GetMapping("/get-goal")
    public ResponseEntity<ApiResponse<DailyIntakeGoal>> getDAilyIntakeGoal(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date targetDate
        ) {
        return ResponseEntity.ok(ApiResponse.success(dailyIntakeGoalService.getDailyIntakeGoal(token, targetDate)));
    }

    @GetMapping("/get-periodic-intake")
    public ResponseEntity<List<MacrosData>> getPeriodicIntake(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
    ) {
        return ResponseEntity.ok(progressTraceService.fetchMacrosData(token, initDate, endDate));
    }

    @GetMapping("/get-daily-intake")
    public ResponseEntity<ApiResponse<MacrosSummary>> getDailyIntake(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date
    ) {
        return ResponseEntity.ok(ApiResponse.success(progressTraceService.fetchDailyMacrosData(token, date)));
    }


    @GetMapping("/get-weekly-intake")
    public ResponseEntity<ApiResponse<List<MacrosSummary>>> getWeeklyIntake(
            @RequestHeader("Authorization") String token,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date
    ) {
        return ResponseEntity.ok(ApiResponse.success(progressTraceService.fetchWeeklyMacrosData(token, date)));
    }


    @GetMapping("/get-monthly-intake")
    public ResponseEntity<ApiResponse<List<MacrosSummary>>> getMonthlyWeeklyIntake(
            @RequestHeader("Authorization") String token,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date
    ) {
        return ResponseEntity.ok(ApiResponse.success(progressTraceService.fetchMonthlyMacrosData(token, date)));
    }

//    @PostMapping("/save-progress")
//    public ResponseEntity<ApiResponse<?>> saveProgress(
//            @RequestHeader("Authorization") String token,
//            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
//            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
//    ) {
//        return ResponseEntity.ok(ApiResponse.success(progressTraceService.savePeriodicProgressTrace(token, initDate, endDate)));
//    }





}