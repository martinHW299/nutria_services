package com.nutria.app.controller;

import com.nutria.app.service.ProgressTraceService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nutrition-progress")
public class ProgressTraceController {

    private final ProgressTraceService progressTraceService;

    @GetMapping("/hello")
    public ResponseEntity<List<?>> hello(
            @RequestHeader("Authorization") String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        return ResponseEntity.ok(progressTraceService.fetchMacrosData(token, initDate, endDate));
    }


}