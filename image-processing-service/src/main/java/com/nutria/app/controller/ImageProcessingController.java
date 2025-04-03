package com.nutria.app.controller;


import com.nutria.app.model.IngestionTrace;
import com.nutria.app.service.IngestionTraceService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/food-trace")
public class ImageProcessingController {

    private final IngestionTraceService ingestionTraceService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<IngestionTrace>> save(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(ingestionTraceService.save(token, payload.get("image"))));
    }

    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<IngestionTrace>>> getIngestionTracePeriod(
            @RequestHeader("Authorization") String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
            ) {
        return ResponseEntity.ok(ApiResponse.success(ingestionTraceService.getIngestionTraceByPeriod(token, initDate, endDate)));
    }

}
