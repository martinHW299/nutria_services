package com.nutria.app.controller;


import com.nutria.app.dto.IngestionTraceDTO;
import com.nutria.app.dto.MacrosDataRequest;
import com.nutria.app.model.FoodImage;
import com.nutria.app.model.IngestionTrace;
import com.nutria.app.model.MacrosData;
import com.nutria.app.service.FoodImageService;
import com.nutria.app.service.IngestionTraceService;
import com.nutria.app.service.MacrosDataService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/food-trace")
public class ImageProcessingController {

    private final IngestionTraceService ingestionTraceService;
    private final MacrosDataService macrosDataService;

    @PostMapping("/save")
    public CompletableFuture<ResponseEntity<ApiResponse<IngestionTrace>>> save(@RequestHeader("Authorization") String token, @RequestBody MacrosDataRequest macrosDataRequest) {
        String image = macrosDataRequest.getImage();
        MacrosData macrosData = macrosDataRequest.getMacrosData();
        return CompletableFuture.completedFuture(ResponseEntity.ok(ApiResponse.success(ingestionTraceService.save(token, image, macrosData))));
    }

    @PostMapping("/processImage")
    public ResponseEntity<ApiResponse<MacrosData>> processImage(@RequestParam("id") int i, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(ApiResponse.success(macrosDataService.getMacrosFromImage(payload.get("image"), i)));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<IngestionTrace>> delete(@RequestParam("intake") Long id) {
        return ResponseEntity.ok(ApiResponse.success(ingestionTraceService.delete(id)));
    }

    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<IngestionTraceDTO>>> getIngestionTracePeriod(
            @RequestHeader("Authorization") String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate
            ) {
        return ResponseEntity.ok(ApiResponse.success(ingestionTraceService.getIngestionTraceByPeriod(token, initDate, endDate)));
    }

}
