package com.nutria.app.controller;

import com.nutria.app.dto.IngestionTraceDTO;
import com.nutria.app.dto.MacrosDataRequest;
import com.nutria.app.dto.AdvancedAnalysisRequest;
import com.nutria.app.dto.AnalyzeAndSaveRequest;
import com.nutria.app.model.IngestionTrace;
import com.nutria.app.model.MacrosData;
import com.nutria.app.service.AiService;
import com.nutria.app.service.IngestionTraceService;
import com.nutria.app.service.MacrosDataService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/food-trace")
public class ImageProcessingController {

    private final IngestionTraceService ingestionTraceService;
    private final MacrosDataService macrosDataService;
    private final AiService aiService;

    // ==================== EXISTING ENDPOINTS (UNCHANGED) ====================

    @PostMapping("/save")
    public CompletableFuture<ResponseEntity<ApiResponse<IngestionTrace>>> save(
            @RequestHeader("Authorization") String token,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestBody MacrosDataRequest macrosDataRequest) {

        String image = macrosDataRequest.getImage();
        MacrosData macrosData = macrosDataRequest.getMacrosData();

        return CompletableFuture.completedFuture(
                ResponseEntity.ok(ApiResponse.success(
                        ingestionTraceService.save(token, date, image, macrosData)
                ))
        );
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<IngestionTrace>> delete(@RequestParam("intake") Long id) {
        return ResponseEntity.ok(ApiResponse.success(ingestionTraceService.delete(id)));
    }

    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<IngestionTraceDTO>>> getIngestionTracePeriod(
            @RequestHeader("Authorization") String token,
            @RequestParam("init") @DateTimeFormat(pattern = "yyyy-MM-dd") Date initDate,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {

        return ResponseEntity.ok(ApiResponse.success(
                ingestionTraceService.getIngestionTraceByPeriod(token, initDate, endDate)
        ));
    }

    // ==================== AI IMAGE PROCESSING ENDPOINTS ====================

    /**
     * Legacy endpoint - maintained for backward compatibility
     * @param useMockData 0 = use mock data, 1 = use AI analysis
     * @param temperature AI model temperature
     * @param payload Contains base64 image using Map<String, String>
     */
    @PostMapping("/process-image")
    public ResponseEntity<ApiResponse<MacrosData>> processImage(
            @RequestParam("id") int useMockData,
            @RequestParam("tmp") double temperature,
            @RequestBody Map<String, String> payload) {

        log.info("Processing image with legacy endpoint - useMockData: {}, temperature: {}", useMockData, temperature);

        // Manual validation
        if (useMockData < 0 || useMockData > 1) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "useMockData must be 0 or 1"));
        }

        if (temperature < 0.0 || temperature > 2.0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Temperature must be between 0.0 and 2.0"));
        }

        String imageBase64 = payload.get("image");
        if (imageBase64 == null || imageBase64.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Image data is required"));
        }

        try {
            MacrosData result = macrosDataService.getMacrosFromImage(imageBase64, useMockData, temperature);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Image processing failed", e);
            return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Image processing failed: " + e.getMessage()));
        }
    }

    /**
     * Advanced AI analysis endpoint with user-specified serving size
     */
    @PostMapping("/analyze-food")
    public ResponseEntity<ApiResponse<MacrosData>> analyzeFoodAdvanced(
            @RequestBody AdvancedAnalysisRequest request) {

        log.info("Analyzing food image - useMockData: {}, userServingSize: {}, temperature: {}",
                request.isUseMockData(), request.getUserServingSize(), request.getTemperature());

        // Manual validation
        if (request.getImage() == null || request.getImage().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Image cannot be blank"));
        }

        if (request.getTemperature() < 0.0 || request.getTemperature() > 2.0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Temperature must be between 0.0 and 2.0"));
        }

        if (request.getUserServingSize() != null && request.getUserServingSize() <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "User serving size must be positive"));
        }

        try {
            MacrosData result = macrosDataService.analyzeFoodImage(
                    request.getImage(),
                    request.isUseMockData(),
                    request.getUserServingSize(),
                    request.getTemperature()
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Food analysis failed", e);
            return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Raw AI service analysis - returns complete AI response structure
     */
    @PostMapping("/analyze-food-raw")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeFoodRaw(
            @RequestBody AdvancedAnalysisRequest request) {

        log.info("Raw AI analysis - userServingSize: {}, temperature: {}",
                request.getUserServingSize(), request.getTemperature());

        // Manual validation
        if (request.getImage() == null || request.getImage().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Image cannot be blank"));
        }

        try {
            Map<String, Object> result = aiService.analyzeFood(
                    request.getImage(),
                    request.getUserServingSize(),
                    request.getTemperature()
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Raw AI analysis failed", e);
            return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Async image processing for better user experience
     */
    @Async
    @PostMapping("/analyze-food-async")
    public CompletableFuture<ResponseEntity<ApiResponse<MacrosData>>> analyzeFoodAsync(
            @RequestBody AdvancedAnalysisRequest request) {

        log.info("Async food analysis started - useMockData: {}", request.isUseMockData());

        // Manual validation
        if (request.getImage() == null || request.getImage().trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Image cannot be blank"))
            );
        }

        try {
            MacrosData result = macrosDataService.analyzeFoodImage(
                    request.getImage(),
                    request.isUseMockData(),
                    request.getUserServingSize(),
                    request.getTemperature()
            );

            return CompletableFuture.completedFuture(
                    ResponseEntity.ok(ApiResponse.success(result))
            );
        } catch (Exception e) {
            log.error("Async food analysis failed", e);
            return CompletableFuture.completedFuture(
                    ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Analysis failed: " + e.getMessage()))
            );
        }
    }

    /**
     * Complete workflow: analyze + save in one call
     */
    @PostMapping("/analyze-and-save")
    public ResponseEntity<ApiResponse<IngestionTrace>> analyzeAndSave(
            @RequestHeader("Authorization") String token,
            @RequestBody AnalyzeAndSaveRequest request) {

        log.info("Analyze and save workflow started");

        // Manual validation
        if (request.getImage() == null || request.getImage().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Image cannot be blank"));
        }

        if (request.getDate() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Date cannot be null"));
        }

        try {
            // 1. Analyze the food image
            MacrosData macrosData = macrosDataService.analyzeFoodImage(
                    request.getImage(),
                    request.isUseMockData(),
                    request.getUserServingSize(),
                    request.getTemperature()
            );

            // 2. Save to ingestion trace
            IngestionTrace trace = ingestionTraceService.save(
                    token,
                    request.getDate(),
                    request.getImage(),
                    macrosData
            );

            return ResponseEntity.ok(ApiResponse.success(trace));
        } catch (Exception e) {
            log.error("Analyze and save workflow failed", e);
            return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Workflow failed: " + e.getMessage()));
        }
    }

    /**
     * Simple batch processing - analyze multiple images with same parameters
     */
    @PostMapping("/analyze-batch-simple")
    public ResponseEntity<ApiResponse<List<MacrosData>>> analyzeBatchSimple(
            @RequestParam("useMockData") boolean useMockData,
            @RequestParam(value = "userServingSize", required = false) Double userServingSize,
            @RequestParam(value = "temperature", defaultValue = "0.7") double temperature,
            @RequestBody List<String> images) {

        log.info("Simple batch analysis started - {} images", images.size());

        // Manual validation
        if (temperature < 0.0 || temperature > 2.0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Temperature must be between 0.0 and 2.0"));
        }

        if (images.size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Maximum 10 images allowed per batch"));
        }

        try {
            List<MacrosData> results = images.stream()
                    .map(imageBase64 -> {
                        try {
                            return macrosDataService.analyzeFoodImage(
                                    imageBase64,
                                    useMockData,
                                    userServingSize,
                                    temperature
                            );
                        } catch (Exception e) {
                            log.error("Failed to analyze image in batch", e);
                            // Return a minimal error response
                            MacrosData errorData = new MacrosData();
                            errorData.setDescription("Analysis failed: " + e.getMessage());
                            errorData.setCalories(0.0);
                            errorData.setProteins(0.0);
                            errorData.setCarbs(0.0);
                            errorData.setFats(0.0);
                            errorData.setServingSize(0.0);
                            return errorData;
                        }
                    })
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Batch analysis failed", e);
            return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Batch analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for AI service
     */
    @GetMapping("/ai-health")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkAiHealth() {
        try {
            // Test with a simple mock analysis
            macrosDataService.analyzeFoodImage("dummy", true, null, 0.0);

            Map<String, String> status = Map.of(
                    "status", "healthy",
                    "aiService", "available",
                    "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("AI health check failed", e);

            Map<String, String> status = Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "AI service unhealthy"));
        }
    }
}