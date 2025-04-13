package com.nutria.app.controller;

import com.nutria.app.dto.MacrosData;
import com.nutria.app.model.DailyIntakeGoal;
import com.nutria.app.service.DailyIntakeGoalService;
import com.nutria.app.service.ProgressTraceService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<ApiResponse<DailyIntakeGoal>> saveDailyGoal(@RequestHeader("Authorization") String token){
        return ResponseEntity.ok(ApiResponse.success(dailyIntakeGoalService.saveDailyIntakeGoal(token)));
    }

    @GetMapping("/get-periodic-intake")
    public ResponseEntity<List<MacrosData>> getPeriodicIntake(
            @RequestHeader("Authorization") String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
    ) {
        return ResponseEntity.ok(progressTraceService.fetchMacrosData(token, initDate, endDate));
    }

    @PostMapping("/save-progress")
    public ResponseEntity<ApiResponse<?>> saveProgress(
            @RequestHeader("Authorization") String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(progressTraceService.savePeriodicProgressTrace(token, initDate, endDate)));
    }





}